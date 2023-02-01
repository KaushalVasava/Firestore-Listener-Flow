/*
 * Copyright (C) 2023 Kaushal Vasava.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package com.kaushalvasava.library.firestorelistenerflow

import android.util.Log
import com.google.firebase.firestore.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @receiver [DocumentReference] of which value needs to be extracted
 * @param fieldPath Optional field path which needs to be parsed instead of complete object. Default value is null
 * @param source What should be the source of data for this [DocumentReference]. See [Source]
 * @return Parsed model or null if data doesn't exist
 */

private const val TAG = "Firestore"
public suspend inline fun <reified M> DocumentReference.toValue(
    fieldPath: FieldPath? = null,
    source: Source = Source.SERVER,
): M? {
    val snapshot = toSnapshotValue(source)
    snapshot ?: return null
    return if (snapshot.exists()) {
        snapshot.convertToObj(fieldPath)
    } else {
        null
    }
}

/**
 * @receiver [DocumentReference] for which we want to monitor the changes
 * @param fieldPath Optional field path which needs to be parsed instead of complete object. Default value null
 * @param excludeCache If true then only server value changes are propagated into the [Flow]
 * else changes from cached value is also propagated
 * @param includeMetaDataChanges Indicates whether metadata-only changes (that is, only [DocumentSnapshot.getMetadata] or
 * [QuerySnapshot.getMetadata] changed) should trigger snapshot events.
 */
public inline fun <reified M : Any> DocumentReference.toValueFlow(
    fieldPath: FieldPath? = null,
    excludeCache: Boolean = true,
    includeMetaDataChanges: Boolean = true,
): Flow<M> {
    return toSnapshotValueFlow(includeMetaDataChanges).filter {
        it.exists()
    }.filter {
        if (!excludeCache) {
            true
        } else {
            excludeCache && !it.metadata.isFromCache
        }
    }.mapNotNull {
        it.convertToObj(fieldPath)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@PublishedApi
internal fun DocumentReference.toSnapshotValueFlow(includeMetaDataChanges: Boolean): Flow<DocumentSnapshot> {
    return callbackFlow {
        val valueEventListener = EventListener<DocumentSnapshot> { snapshot, exception ->
            snapshot?.let { documentSnapshot ->
                // Success case
                Log.d(TAG, "[SnapShotValueFlow] onDataChange")
                trySend(documentSnapshot).onFailure {
                    if (!this.isClosedForSend) {
                        // Handle exception from the channel: failure in sending data in the flow
                        Log.e(
                            TAG,
                            RuntimeException(
                                "Failure in sending data in the flow",
                                it ?: RuntimeException("Null error returned")
                            ).message.toString()
                        )
                    }
                }
            }

            exception?.let {
                // Error case
                Log.d(TAG, "[SnapShotValueFlow] onCancelled")
                Log.e(TAG, exception.message.toString())
                cancel(CancellationException("[SnapShotValueFlow] API Error", exception))
            }
        }
        val registrationListener =
            this@toSnapshotValueFlow.addSnapshotListener(
                if (includeMetaDataChanges) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE,
                valueEventListener
            )

        awaitClose {
            Log.d(TAG, "[toValueFlow] awaitClose called")
            registrationListener.remove()
        }
    }
}

@PublishedApi
internal inline fun <reified M> DocumentSnapshot.convertToObj(fieldPath: FieldPath?): M? {
    return fieldPath?.let {
        this[it, M::class.java]
    } ?: this.toObject(M::class.java)
}

@PublishedApi
internal suspend fun DocumentReference.toSnapshotValue(source: Source): DocumentSnapshot? {
    return suspendCancellableCoroutine { cancellableContinuation ->
        this.get(source).addOnCompleteListener { task ->
            if (!cancellableContinuation.isActive) {
                return@addOnCompleteListener
            }
            if (task.isSuccessful) {
                Log.d(TAG, "[SnapshotValue] onDataChange: data = ${task.result}")
                cancellableContinuation.resume(task.result)
            } else {
                Log.d(TAG, "[SnapshotValue] onCancelled")
                Log.e(TAG, task.exception.toString())
                cancellableContinuation.resumeWithException(
                    task.exception ?: RuntimeException("Null exception from Firestore")
                )
            }
        }

        cancellableContinuation.invokeOnCancellation {
            Log.d(TAG, "[SnapshotValue] invokeOnCancellation")
        }
    }
}
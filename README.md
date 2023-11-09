# Firestore-Listener-Flow

It is a library for simplifying Firestore listener callbacks to Kotlin flow and suspend function to easily manage and use callbacks.

Firebase callbacks like addSnapshotListener and addOnCompleteListener are simplified using Flows so you can easily write clean code. 

# Steps
1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```
allprojects {
	repositories {
	     ...
             maven { url 'https://jitpack.io' }
	}
}
```

2. Add the dependency
```
dependencies {
      implementation 'com.github.KaushalVasava:firestore-listener-flow:1.0'
}
```

# Demo Example
You can use now toValueFlow<Cart> instead of addOnSnapshotListener.
```
  val cartFlow: Flow<BaseState<Cart>> = 
        Firebase.firestore.collection("users").document(userId)
            .collection("cart").document(userId).toValueFlow<Cart>()
            .map { cart ->
                BaseState.Success(
                    Cart(
                        items = cart.items.entries.associate {
                            it.key to BaseState.Success(it.value)
                        },
                        cartId = cart.id,
                        addressId = cart.addressId
                    )
                )
          }
```
# Technology and Build 
- Kotlin
- Firebase firestore
- JDK-17
- Kotlin 1.9.20 
- Gradle 8.1

# Licence
Copyright 2023 Kaushal Vasava

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

# Author
Kaushal Vasava

If you find any issues or want to give some suggestions then contact me on LinkedIn, Twitter or Instagram.

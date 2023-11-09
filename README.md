# Firestore-Listener-Flow

It is a library for simplify firestore listener callbacks to kotlin flow and suspend function to easily manage and use callbacks.

Firebase callbacks like addSnapshotListener, addOnCompleteListener are simplified using Flows so you can easily write clean code.

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
# Author
Kaushal Vasava

If you find any issues or want to give some suggestions then contact me on LinkedIn, Twitter or Instagram.

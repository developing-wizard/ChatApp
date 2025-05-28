package com.example.chatapp

import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX


fun FragmentActivity.requestPermissionss(array: Array<String>, onAllGranted: () -> Unit) {
    PermissionX.init(this)
        .permissions(*array)
        .request { allGranted, grantedList, deniedList ->
            if (allGranted) {
                onAllGranted()
            }
        }
}

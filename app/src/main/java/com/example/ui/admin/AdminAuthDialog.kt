package com.example.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AdminAuthResult
import com.example.data.security.AdminSecurityManager

@Composable
fun AdminAuthDialog(
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = remember { AdminSecurityManager.getInstance(context) }

    var pin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val isLockedOut = securityManager.isLockedOut()
    val isDefaultPin = securityManager.isDefaultPinInUse()

    val handleConfirm = {
        val result = securityManager.authenticate(pin)
        when (result) {
            is AdminAuthResult.Success -> {
                errorText = null
                onAuthenticated()
            }
            is AdminAuthResult.InvalidPin -> {
                errorText = if (result.remainingAttempts > 0) {
                    "Incorrect Admin PIN. ${result.remainingAttempts} attempt(s) remaining."
                } else {
                    "Incorrect PIN. Account temporarily locked out."
                }
            }
            is AdminAuthResult.LockedOut -> {
                errorText = "Too many failed attempts. Locked out for ${result.remainingSeconds}s."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Admin Access Portal",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Please enter your administrator PIN to access the Question Bank and question management system.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8) {
                            pin = it
                            errorText = null
                        }
                    },
                    label = { Text("Admin PIN") },
                    placeholder = { Text(if (isDefaultPin) "Enter PIN (Default: 1234)" else "Enter your Admin PIN") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    },
                    isError = errorText != null,
                    supportingText = {
                        if (errorText != null) {
                            Text(text = errorText!!, color = MaterialTheme.colorScheme.error)
                        } else if (isLockedOut) {
                            Text(
                                text = "Security lockout active (${securityManager.getRemainingLockoutSeconds()}s remaining)",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        } else if (isDefaultPin) {
                            Text(text = "Default PIN: 1234 (Configurable in Admin Settings)", fontSize = 11.sp)
                        } else {
                            Text(text = "Secured with salted cryptographic authentication", fontSize = 11.sp)
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (!isLockedOut) handleConfirm() }),
                    singleLine = true,
                    enabled = !isLockedOut,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = handleConfirm,
                enabled = !isLockedOut && pin.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("admin_login_confirm_btn")
            ) {
                Text("Unlock Admin", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("admin_login_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

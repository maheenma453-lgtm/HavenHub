package com.example.havenhub.components
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.KeyboardCapitalization

/**
 * Reusable text field for HavenHub forms.
 *
 * Handles:
 *  - Standard text / email / phone / number inputs
 *  - Password fields with show/hide toggle
 *  - Inline error messages
 *  - Leading and trailing icons
 *
 * @param value           Current input value
 * @param onValueChange   Value change callback
 * @param label           Floating label text
 * @param modifier        Optional modifier
 * @param placeholder     Hint text shown inside the field
 * @param leadingIcon     Optional leading icon
 * @param trailingIcon    Optional trailing icon (ignored for password fields)
 * @param isPassword      Enables password masking with visibility toggle
 * @param errorMessage    Non-null string shows the field in error state
 * @param keyboardType    Input keyboard type
 * @param imeAction       IME action button
 * @param onImeAction     Callback for IME action (e.g. Done / Next)
 * @param singleLine      Constrains input to one line
 * @param maxLines        Max visible lines (used when singleLine = false)
 * @param enabled         Whether the field is interactive
 * @param readOnly        Shows value but prevents editing
 */
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val visualTransformation = when {
        isPassword && !passwordVisible -> PasswordVisualTransformation()
        else -> VisualTransformation.None
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (errorMessage != null)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingIcon = when {
            isPassword -> {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            }
            trailingIcon != null -> {
                { Icon(imageVector = trailingIcon, contentDescription = null) }
            }
            else -> null
        },
        visualTransformation = visualTransformation,
        isError = errorMessage != null,
        supportingText = errorMessage?.let {
            { Text(it, color = MaterialTheme.colorScheme.error) }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
            capitalization = when (keyboardType) {
                KeyboardType.Email, KeyboardType.Password, KeyboardType.Number,
                KeyboardType.Phone, KeyboardType.Uri -> KeyboardCapitalization.None
                else -> KeyboardCapitalization.Sentences
            }
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else maxLines,
        enabled = enabled,
        readOnly = readOnly,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}



@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mckaifu.app.data.model.AppSettings
import com.mckaifu.app.ui.theme.*
import com.mckaifu.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    isSettingPassword: Boolean = false,
    onSuccess: () -> Unit,
    navController: NavController,
    vm: MainViewModel = viewModel()
) {
    val settings by vm.settings.collectAsState()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ZalithGradientStart, ZalithBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                null,
                modifier = Modifier.size(80.dp),
                tint = ZalithPrimary
            )
            Spacer(Modifier.height(24.dp))

            Text(
                if (isSettingPassword) "设置访问密码" else "输入访问密码",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isSettingPassword) "设置密码保护你的服务器管理面板"
                else "此应用已启用密码保护",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility, null
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isSettingPassword) ImeAction.Next else ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZalithPrimary,
                    unfocusedBorderColor = ZalithCardBorder,
                    focusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = ZalithPrimary
                )
            )

            if (isSettingPassword) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("确认密码") },
                    leadingIcon = { Icon(Icons.Filled.LockReset, null) },
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZalithPrimary,
                        unfocusedBorderColor = ZalithCardBorder,
                        focusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = ZalithSurfaceVariant.copy(alpha = 0.3f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ZalithPrimary
                    )
                )
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = ServerError, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isSettingPassword) {
                        if (password.length < 4) {
                            error = "密码至少需要4个字符"
                        } else if (password != confirmPassword) {
                            error = "两次输入的密码不一致"
                        } else {
                            vm.updateSettings(settings.copy(
                                passwordEnabled = true,
                                passwordHash = password // In production, use proper hashing
                            ))
                            onSuccess()
                        }
                    } else {
                        if (password == settings.passwordHash) {
                            onSuccess()
                        } else {
                            error = "密码错误，请重试"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZalithPrimary
                )
            ) {
                Icon(
                    if (isSettingPassword) Icons.Filled.Save else Icons.Filled.LockOpen,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isSettingPassword) "保存密码" else "解锁",
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isSettingPassword) {
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        vm.updateSettings(settings.copy(passwordEnabled = false))
                        onSuccess()
                    }
                ) {
                    Text("跳过密码", color = TextSecondary)
                }
            }
        }
    }
}

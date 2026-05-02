package com.example.scms.ui.screens
import com.example.scms.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*
import com.example.scms.ui.screens.* 
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Colors are now handled by MaterialTheme for Dark/Light support
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    // Form State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf("") }

    // OTP State
    var isPhoneMode by remember { mutableStateOf(false) }
    var isOtpSent by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf("") }

    // Firebase Phone Auth Callbacks
    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        isLoading = false
                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                    }
                    .addOnFailureListener {
                        errorMessage = it.message ?: "Authentication failed"
                        isLoading = false
                    }
            }
            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                errorMessage = e.message ?: "Verification failed. Ensure SHA-1 is in Firebase Console."
            }
            override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                isLoading = false
                verificationId = verId
                isOtpSent = true
                resetMessage = "OTP Sent Successfully!"
            }
        }
    }

    // Animation States
    var isVisible by remember { mutableStateOf(false) }
    val fadeAnim by animateFloatAsState(if (isVisible) 1f else 0f, tween(1200))
    val slideAnim by animateDpAsState(if (isVisible) 0.dp else 40.dp, tween(1000, delayMillis = 100, easing = EaseOutQuart))

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        // 🌌 Atmospheric Glows
        Box(modifier = Modifier.size(400.dp).align(Alignment.TopEnd).offset(x = 150.dp, y = (-100).dp).blur(120.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape))
        Box(modifier = Modifier.size(300.dp).align(Alignment.BottomStart).offset(x = (-100).dp, y = 100.dp).blur(100.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            /* 🏷️ FULL BRANDING (Hero Section) */
            Column(
                modifier = Modifier.graphicsLayer { alpha = fadeAnim; translationY = slideAnim.toPx() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    color = Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "SMART COMPLAINT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Management System",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
                Text(
                    "Your City, Your Voice.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(48.dp))

            /* ✨ GLASSMORPHISM LOGIN CARD */
            Surface(
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = fadeAnim; translationY = slideAnim.toPx() * 0.5f },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("SIGN IN", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = { isPhoneMode = false; errorMessage = ""; resetMessage = "" }) {
                            Text("Email", color = if (!isPhoneMode) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = if (!isPhoneMode) FontWeight.Black else FontWeight.Normal)
                        }
                        TextButton(onClick = { isPhoneMode = true; errorMessage = ""; resetMessage = "" }) {
                            Text("Phone (OTP)", color = if (isPhoneMode) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = if (isPhoneMode) FontWeight.Black else FontWeight.Normal)
                        }
                    }

                    if (!isPhoneMode) {
                        AuthField(
                            value = email, 
                            onValueChange = { email = it }, 
                            label = "Email Address", 
                            icon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email
                        )
                        Spacer(Modifier.height(16.dp))
                        AuthField(
                            value = password, 
                            onValueChange = { password = it }, 
                            label = "Password", 
                            icon = Icons.Default.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible }
                        )
                        /* 🔑 FORGOT PASSWORD LINK */
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                "Forgot Password?",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    if (email.isBlank()) {
                                        errorMessage = "Enter your email first"
                                    } else {
                                        isLoading = true
                                        auth.sendPasswordResetEmail(email.trim())
                                            .addOnSuccessListener {
                                                isLoading = false
                                                resetMessage = "Reset link sent to your email!"
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
                                                errorMessage = it.message ?: "Failed to send reset link"
                                            }
                                    }
                                }
                            )
                        }
                    } else {
                        if (!isOtpSent) {
                            AuthField(
                                value = phone, 
                                onValueChange = { phone = it }, 
                                label = "Phone Number (+91...)", 
                                icon = Icons.Default.Phone,
                                keyboardType = KeyboardType.Phone
                            )
                        } else {
                            AuthField(
                                value = otp, 
                                onValueChange = { otp = it }, 
                                label = "Enter 6-Digit OTP", 
                                icon = Icons.Default.Lock,
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
                    }
                    if (resetMessage.isNotEmpty()) {
                        Text(resetMessage, color = Color(0xFF22C55E), fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
                    }

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            errorMessage = ""
                            resetMessage = ""
                            if (!isPhoneMode) {
                                if (email.isEmpty() || password.isEmpty()) {
                                    errorMessage = "Please fill all fields"
                                    return@Button
                                }
                                isLoading = true
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                    }
                                    .addOnFailureListener {
                                        errorMessage = it.message ?: "Authentication failed"
                                        isLoading = false
                                    }
                            } else {
                                if (!isOtpSent) {
                                    if (phone.isEmpty()) {
                                        errorMessage = "Please enter a valid phone number"
                                        return@Button
                                    }
                                    val formattedPhone = when {
                                        phone.startsWith("+") -> phone
                                        phone.length == 10 -> "+91$phone"
                                        else -> phone
                                    }
                                    val options = PhoneAuthOptions.newBuilder(auth)
                                        .setPhoneNumber(formattedPhone)
                                        .setTimeout(60L, TimeUnit.SECONDS)
                                        .setActivity(context as Activity)
                                        .setCallbacks(callbacks)
                                        .build()
                                    PhoneAuthProvider.verifyPhoneNumber(options)
                                } else {
                                    if (otp.isEmpty()) {
                                        errorMessage = "Please enter the OTP"
                                        return@Button
                                    }
                                    isLoading = true
                                    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                                    auth.signInWithCredential(credential)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                        }
                                        .addOnFailureListener {
                                            errorMessage = it.message ?: "Invalid OTP"
                                            isLoading = false
                                        }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        } else {
                            val btnText = if (!isPhoneMode) "ENTER THE SENTINEL" else if (!isOtpSent) "SEND OTP" else "VERIFY & LOGIN"
                            Text(btnText, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            /* 🔄 FOOTER */
            Row(
                modifier = Modifier.padding(bottom = 40.dp).graphicsLayer { alpha = fadeAnim },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New Citizen? ", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                Text(
                    "Join Now", 
                    color = MaterialTheme.colorScheme.primary, 
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { navController.navigate("signup") }
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onTogglePassword) {
                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}

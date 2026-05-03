package com.example.scms.ui.screens
import com.example.scms.*
import com.example.scms.ui.components.* 
import com.example.scms.data.model.* 
import com.example.scms.data.network.* 
import com.example.scms.utils.* 
import com.example.scms.service.*

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

// SCMS Branding Tokens
private val AccentGreen = Color(0xFF10B981)
private val SoftGreen = Color(0xFF34D399)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Form State
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Animation States
    var isVisible by remember { mutableStateOf(false) }
    val fadeAnim by animateFloatAsState(if (isVisible) 1f else 0f, tween(1200))
    val slideAnim by animateDpAsState(if (isVisible) 0.dp else 40.dp, tween(1000, delayMillis = 100, easing = EaseOutQuart))

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        
        // 🌌 Atmospheric Glows
        Box(modifier = Modifier.size(400.dp).align(Alignment.TopStart).offset(x = (-150).dp, y = (-100).dp).blur(120.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape))
        Box(modifier = Modifier.size(300.dp).align(Alignment.BottomEnd).offset(x = 100.dp, y = 100.dp).blur(100.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            /* 🏷️ BRANDING SECTION */
            Column(
                modifier = Modifier.graphicsLayer { alpha = fadeAnim; translationY = slideAnim.toPx() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    color = Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "JOIN SCMS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Smart Complaint",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Management System",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(40.dp))

            /* ✨ GLASSMORPHISM SIGNUP CARD */
            Surface(
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = fadeAnim; translationY = slideAnim.toPx() * 0.5f },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("REGISTRATION", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(20.dp))

                    AuthField(
                        value = name, 
                        onValueChange = { name = it }, 
                        label = "Full Name", 
                        icon = Icons.Default.Person
                    )

                    Spacer(Modifier.height(12.dp))

                    AuthField(
                        value = phone, 
                        onValueChange = { phone = it }, 
                        label = "Mobile Number", 
                        icon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone
                    )

                    Spacer(Modifier.height(12.dp))

                    AuthField(
                        value = email, 
                        onValueChange = { email = it }, 
                        label = "Email Address", 
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(Modifier.height(12.dp))

                    AuthField(
                        value = password, 
                        onValueChange = { password = it }, 
                        label = "Password", 
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible }
                    )

                    Spacer(Modifier.height(12.dp))

                    AuthField(
                        value = confirmPassword, 
                        onValueChange = { confirmPassword = it }, 
                        label = "Confirm Password", 
                        icon = Icons.Default.LockReset,
                        isPassword = true,
                        passwordVisible = confirmPasswordVisible,
                        onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible }
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(errorMessage, color = Color.Red.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
                    }

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                errorMessage = "Please fill all fields"
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@Button
                            }
                            
                            isLoading = true
                            errorMessage = ""
                            
                            scope.launch {
                                try {
                                    val response = RetrofitClient.api.signup(mapOf(
                                        "name" to name,
                                        "phone" to phone,
                                        "email" to email.trim().toLowerCase(),
                                        "password" to password
                                    ))
                                    
                                    if (response.isSuccessful && response.body()?.get("success") == true) {
                                        // Create Firebase user for notifications/auth
                                        auth.createUserWithEmailAndPassword(email.trim(), password)
                                            .addOnSuccessListener { fbResult ->
                                                val newUser = User(
                                                    id = 0, // Backend will provide or we fetch
                                                    firebaseUid = fbResult.user?.uid ?: "",
                                                    name = name,
                                                    phone = phone,
                                                    email = email,
                                                    points = 0
                                                )
                                                // Fetch fresh user data to get the assigned ID
                                                scope.launch {
                                                    val loginResp = RetrofitClient.api.login(mapOf("email" to email.trim(), "password" to password))
                                                    if (loginResp.isSuccessful) {
                                                        val userData = loginResp.body()?.user
                                                        if (userData != null) {
                                                            UserSession.currentUser = userData
                                                            // ✅ PERSIST SESSION
                                                            SessionManager(context).saveUser(userData)
                                                            isLoading = false
                                                            navController.navigate("home") { popUpTo("signup") { inclusive = true } }
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnFailureListener {
                                                errorMessage = "Firebase Error: ${it.message}"
                                                isLoading = false
                                            }
                                    } else {
                                        errorMessage = "Signup failed: Email or Phone might already exist"
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Connection Error: ${e.message}"
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        else Text("CREATE ACCOUNT", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            /* 🔄 FOOTER */
            Row(
                modifier = Modifier.padding(bottom = 40.dp).graphicsLayer { alpha = fadeAnim },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already a citizen? ", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                Text(
                    "Login", 
                    color = MaterialTheme.colorScheme.primary, 
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

package com.example.vidplay.ui.streaming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.vidplay.Navigation.Routes
import com.example.vidplay.util.PreferenceHelper
import com.example.vidplay.ui.components.CustomOutlinedTextField

@Composable
fun TokenTakenPageScreen(navController: NavController = rememberNavController()) {
	var text by remember { mutableStateOf("") }

	val context = LocalContext.current

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(start = 24.dp, end = 24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		val boxShape = RoundedCornerShape(8.dp)
		val focusManager = LocalFocusManager.current
		val keyboardController = LocalSoftwareKeyboardController.current

		OutlinedTextField(
			value = text,
			onValueChange = { text = it },
			modifier = Modifier.fillMaxWidth(),
			placeholder = { Text("Enter token") },
			singleLine = false,
			keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
			keyboardActions = KeyboardActions(
				onDone = {
					val token = text.trim()
					if (token.isNotEmpty()) {
						PreferenceHelper(context).token = token
						focusManager.clearFocus()
						keyboardController?.hide()
						navController.navigate(Routes.STREAMING) {
							popUpTo(Routes.TOKEN_PAGE) { inclusive = true }
						}
					}
				}
			)
		)

		Spacer(modifier = Modifier.height(16.dp))

		Button(
			onClick = {
				val token = text.trim()
				if (token.isNotEmpty()) {
					PreferenceHelper(context).token = token
					navController.navigate(Routes.STREAMING) {
						// Clear the token page from back-stack so Back doesn't return to it
						popUpTo(Routes.TOKEN_PAGE) { inclusive = true }
					}
				}
			},
			modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
			shape = boxShape,
			colors = ButtonDefaults.buttonColors(
				containerColor = Color(0xFF6200EE),
				contentColor = Color.White
			)
		) {
			Text("Submit")
		}
	}
}

@Preview(showBackground = true)
@Composable
fun TokenTakenPagePreview() {
	TokenTakenPageScreen(navController = rememberNavController())
}


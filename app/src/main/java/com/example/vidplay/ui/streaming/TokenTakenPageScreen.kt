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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TokenTakenPageScreen() {
	var text by remember { mutableStateOf("") }

	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(start = 24.dp, end = 24.dp),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		val boxShape = RoundedCornerShape(8.dp)

		OutlinedTextField(
			value = text,
			onValueChange = { text = it },
            shape = boxShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (3 * 24).dp),
			label = { Text("Enter token") },
			singleLine = false,
			minLines = 3,
			maxLines = Int.MAX_VALUE,
		)

		Spacer(modifier = Modifier.height(16.dp))

		Button(
			onClick = { /* handle click */ },
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
	TokenTakenPageScreen()
}


package com.example.vidplay.ui.streaming

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.Color
import com.example.vidplay.ui.components.CustomOutlinedTextField

/**
 * Dialog to collect details for starting a stream.
 * onSubmit returns title, description and optional thumbnail Uri string.
 */
@Composable
fun StartStreamDialog(
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    onSubmit: (title: String, description: String, thumbnailUri: String?) -> Unit
) {
	var title by remember { mutableStateOf("") }
	var description by remember { mutableStateOf("") }
	var thumbnail by remember { mutableStateOf<Uri?>(null) }

	val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		if (uri != null) thumbnail = uri
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			Button(
				onClick = { if (!isLoading) onSubmit(title.trim(), description.trim(), thumbnail?.toString()) },
				enabled = title.isNotBlank() && !isLoading,
				colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE), contentColor = Color.White)
			) {
				if (isLoading) {
					CircularProgressIndicator(
						modifier = Modifier.size(18.dp),
						strokeWidth = 2.dp,
						color = Color.White
					)
					Spacer(Modifier.size(8.dp))
					Text("Starting...")
				} else {
					Text("Start")
				}
			}
		},
		dismissButton = {
			Button(onClick = onDismiss) { Text("Cancel") }
		},
		title = { Text("Start New Stream") },
		text = {
			Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
				CustomOutlinedTextField(
					value = title,
					onValueChange = { title = it },
					placeholder = { Text("Title") },
					modifier = Modifier.fillMaxWidth()
				)

				CustomOutlinedTextField(
					value = description,
					onValueChange = { description = it },
					placeholder = { Text("Description") },
					modifier = Modifier.fillMaxWidth()
				)

				Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
					Button(onClick = { launcher.launch("image/*") }) { Text("Choose thumbnail") }
					Spacer(modifier = Modifier.size(12.dp))
					if (thumbnail != null) {
						Image(
							painter = rememberAsyncImagePainter(thumbnail),
							contentDescription = "thumbnail",
							modifier = Modifier.size(64.dp)
						)
					}
				}
			}
		}
	)
}


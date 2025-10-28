package com.appstentcompose.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appversation.appstentcompose.AppstentView
import com.appversation.appstentcompose.ViewContentRepository
import org.json.JSONObject

@Composable
fun TestWidget() {

    val viewModel: AppstentViewModel = viewModel(
        key = "Coherent/LabResultCard",
        factory = AppstentViewModelFactory(
            ViewContentRepository(),
            "Coherent/LabResultCard"
        )
    )

    val viewContent by viewModel.viewContent.collectAsStateWithLifecycle(initialValue = JSONObject())

    Column(
        modifier = Modifier
            .background(Color.Gray.copy(alpha = 0.05f))
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header with title and View All link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Lab Results",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.weight(1f))

        }

        // Horizontal carousel of lab results
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            //create dummy integer array of size 5
            val items = List(5) { it }
            items(items.take(5)) { result ->
                Box(modifier = Modifier.size(200.dp)) {
                    AppstentView(
                        viewContent = viewContent,
                        customContentDataProvider = TestDataProvider()
                    )
                }
            }
        }
    }
}
@file:OptIn(ExperimentalMaterial3Api::class)

package com.mckaifu.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mckaifu.app.data.model.ServerInstance
import com.mckaifu.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectDialog(
    title: String,
    servers: List<ServerInstance>,
    currentServerId: String,
    onDismiss: () -> Unit,
    onSelect: (ServerInstance) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZalithSurface,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            if (servers.isEmpty()) {
                Text("还没有服务器，请先创建服务器", color = TextSecondary)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    servers.forEach { server ->
                        val selected = server.id == currentServerId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(server) },
                            shape = MaterialTheme.shapes.small,
                            color = if (selected) ZalithPrimary.copy(alpha = 0.12f)
                            else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Dns, null,
                                    tint = if (selected) ZalithPrimary else TextSecondary,
                                    modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(server.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${server.coreType.displayName} ${server.coreVersion}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Filled.Check, null, tint = ZalithPrimary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

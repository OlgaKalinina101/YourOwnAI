package com.yourown.ai.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yourown.ai.R
import com.yourown.ai.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryClusteringDialog(
    clusteringStatus: ClusteringStatus,
    biographyStatus: BiographyGenerationStatus,
    memoryCleaningStatus: MemoryCleaningStatus,
    selectedModel: ModelProvider?,
    biography: UserBiography?,
    onStartClustering: () -> Unit,
    onSelectModel: () -> Unit,
    onGenerateBiography: () -> Unit,
    onCancelBiography: () -> Unit,
    onCleanMemories: () -> Unit,
    onCancelCleaning: () -> Unit,
    onViewBiography: () -> Unit,
    onDismiss: () -> Unit
) {
    var showCleanConfirmation by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.memory_clustering_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.memory_clustering_close))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                when (clusteringStatus) {
                    is ClusteringStatus.Idle -> {
                        // Show start button
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Analyze your memories and group them by themes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(onClick = onStartClustering) {
                                Icon(Icons.Default.AutoAwesome, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.memory_clustering_start))
                            }
                        }
                    }
                    
                    is ClusteringStatus.Processing -> {
                        // Show progress
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                progress = clusteringStatus.progress / 100f
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = stringResource(R.string.memory_clustering_analyzing),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = clusteringStatus.step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${clusteringStatus.progress}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    is ClusteringStatus.Completed -> {
                        // Show results
                        ClusteringResultView(
                            result = clusteringStatus.result,
                            biographyStatus = biographyStatus,
                            memoryCleaningStatus = memoryCleaningStatus,
                            selectedModel = selectedModel,
                            biography = biography,
                            onStartAgain = onStartClustering,
                            onSelectModel = onSelectModel,
                            onGenerateBiography = onGenerateBiography,
                            onCancelBiography = onCancelBiography,
                            onCleanMemories = { showCleanConfirmation = true },
                            onCancelCleaning = onCancelCleaning,
                            onViewBiography = onViewBiography
                        )
                    }
                    
                    is ClusteringStatus.Failed -> {
                        // Show error
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = stringResource(R.string.memory_clustering_error, clusteringStatus.error),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(onClick = onStartClustering) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
        
        // Clean Memory Confirmation Dialog
        if (showCleanConfirmation) {
            AlertDialog(
                onDismissRequest = { showCleanConfirmation = false },
                icon = {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { 
                    Text(stringResource(R.string.memory_clean_confirm_title)) 
                },
                text = { 
                    Column {
                        Text(stringResource(R.string.memory_clean_confirm_message))
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = stringResource(R.string.memory_clean_confirm_recommendation),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.memory_clean_confirm_steps),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCleanConfirmation = false
                            onCleanMemories()
                        }
                    ) {
                        Text(stringResource(R.string.memory_clean_start))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCleanConfirmation = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ClusteringResultView(
    result: ClusteringResult,
    biographyStatus: BiographyGenerationStatus,
    memoryCleaningStatus: MemoryCleaningStatus,
    selectedModel: ModelProvider?,
    biography: UserBiography?,
    onStartAgain: () -> Unit,
    onSelectModel: () -> Unit,
    onGenerateBiography: () -> Unit,
    onCancelBiography: () -> Unit,
    onCleanMemories: () -> Unit,
    onCancelCleaning: () -> Unit,
    onViewBiography: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Biography Status Card (NEW)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = biography != null && !biography.isEmpty()) { 
                    onViewBiography() 
                },
            shape = RoundedCornerShape(12.dp),
            color = if (biography != null && !biography.isEmpty()) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (biography != null && !biography.isEmpty()) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.biography_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (biography != null && !biography.isEmpty()) {
                            stringResource(R.string.biography_status_ready)
                        } else {
                            stringResource(R.string.biography_status_empty)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (biography != null && !biography.isEmpty()) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                if (biography != null && !biography.isEmpty()) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.memory_clustering_found, result.clusters.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Total memories: ${result.totalMemories}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = stringResource(R.string.memory_cluster_avg_size, result.getAverageClusterSize()),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (result.outliers != null) {
                    Text(
                        text = "Outliers: ${result.outliers.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Clusters list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main clusters
            items(result.clusters.sortedByDescending { it.priorityScore }) { cluster ->
                ClusterCard(cluster = cluster)
            }
            
            // Outliers
            if (result.outliers != null) {
                item {
                    ClusterCard(
                        cluster = result.outliers,
                        isOutliers = true
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Biography generation section
        when (biographyStatus) {
            is BiographyGenerationStatus.Idle -> {
                // Biography actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Select Model button
                    OutlinedButton(
                        onClick = onSelectModel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ModelTraining, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedModel) {
                                is ModelProvider.API -> "Model: ${selectedModel.displayName}"
                                is ModelProvider.Local -> "Model: ${selectedModel.model.displayName}"
                                null -> stringResource(R.string.biography_select_model)
                            }
                        )
                    }
                    
                    // Generate Biography button
                    Button(
                        onClick = onGenerateBiography,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedModel != null
                    ) {
                        Icon(Icons.Default.Person, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (biography != null && !biography.isEmpty()) {
                                stringResource(R.string.biography_update)
                            } else {
                                stringResource(R.string.biography_generate)
                            }
                        )
                    }
                    
                    // View Biography button (if exists)
                    if (biography != null && !biography.isEmpty()) {
                        OutlinedButton(
                            onClick = onViewBiography,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Article, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.biography_view))
                        }
                        
                        // Memory cleaning section (show if biography exists)
                        when (memoryCleaningStatus) {
                            is MemoryCleaningStatus.Idle -> {
                                OutlinedButton(
                                    onClick = onCleanMemories,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = selectedModel != null
                                ) {
                                    Icon(Icons.Default.CleaningServices, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.memory_clean))
                                }
                            }
                            
                            is MemoryCleaningStatus.Processing -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    LinearProgressIndicator(
                                        progress = memoryCleaningStatus.currentCluster.toFloat() / memoryCleaningStatus.totalClusters,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = stringResource(R.string.memory_cleaning),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Text(
                                        text = stringResource(
                                            R.string.memory_cleaning_cluster,
                                            memoryCleaningStatus.currentCluster,
                                            memoryCleaningStatus.totalClusters
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedButton(
                                        onClick = onCancelCleaning,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Close, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.memory_clean_cancel))
                                    }
                                }
                            }
                            
                            is MemoryCleaningStatus.Completed -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.memory_cleaning_completed),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(
                                                R.string.memory_cleaning_completed_description,
                                                memoryCleaningStatus.removedMemories,
                                                memoryCleaningStatus.mergedMemories
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            
                            is MemoryCleaningStatus.Failed -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = memoryCleaningStatus.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Restart analysis button
                    OutlinedButton(
                        onClick = onStartAgain,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Again")
                    }
                }
            }
            
            is BiographyGenerationStatus.Processing -> {
                // Show progress
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = biographyStatus.currentCluster.toFloat() / biographyStatus.totalClusters,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.biography_generating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = stringResource(
                            R.string.biography_processing_cluster,
                            biographyStatus.currentCluster,
                            biographyStatus.totalClusters
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cancel button
                    OutlinedButton(
                        onClick = onCancelBiography,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.biography_cancel))
                    }
                }
            }
            
            is BiographyGenerationStatus.Completed -> {
                // Show success, view button, and clean memory option
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Biography generated successfully!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Button(
                        onClick = onViewBiography,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Article, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.biography_view))
                    }
                    
                    // Memory cleaning section
                    when (memoryCleaningStatus) {
                        is MemoryCleaningStatus.Idle -> {
                            OutlinedButton(
                                onClick = onCleanMemories,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedModel != null
                            ) {
                                Icon(Icons.Default.CleaningServices, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.memory_clean))
                            }
                        }
                        
                        is MemoryCleaningStatus.Processing -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = memoryCleaningStatus.currentCluster.toFloat() / memoryCleaningStatus.totalClusters,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = stringResource(R.string.memory_cleaning),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = stringResource(
                                        R.string.memory_cleaning_cluster,
                                        memoryCleaningStatus.currentCluster,
                                        memoryCleaningStatus.totalClusters
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedButton(
                                    onClick = onCancelCleaning,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Close, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.memory_clean_cancel))
                                }
                            }
                        }
                        
                        is MemoryCleaningStatus.Completed -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.memory_cleaning_completed),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.memory_cleaning_completed_description,
                                            memoryCleaningStatus.removedMemories,
                                            memoryCleaningStatus.mergedMemories
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        
                        is MemoryCleaningStatus.Failed -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = memoryCleaningStatus.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            is BiographyGenerationStatus.Failed -> {
                // Show error
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.biography_error, biographyStatus.error),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Button(
                        onClick = onGenerateBiography,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
private fun ClusterCard(
    cluster: MemoryCluster,
    isOutliers: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isOutliers) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            when {
                cluster.priorityScore > 0.7f -> MaterialTheme.colorScheme.errorContainer
                cluster.priorityScore > 0.4f -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        },
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cluster.getAgeEmoji(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (isOutliers) {
                            Text(
                                text = stringResource(R.string.memory_cluster_outliers, cluster.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Cluster ${cluster.id + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    if (isOutliers) {
                        Text(
                            text = stringResource(R.string.memory_cluster_outliers_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = when (cluster.getPriorityCategory()) {
                                "High" -> stringResource(R.string.memory_cluster_priority_high)
                                "Medium" -> stringResource(R.string.memory_cluster_priority_medium)
                                else -> stringResource(R.string.memory_cluster_priority_low)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) {
                        stringResource(R.string.memory_cluster_collapse)
                    } else {
                        stringResource(R.string.memory_cluster_expand)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricChip(
                    label = stringResource(R.string.memory_cluster_size, cluster.size),
                    icon = Icons.Default.Folder
                )
                
                if (!isOutliers) {
                    MetricChip(
                        label = stringResource(R.string.memory_cluster_age, cluster.avgAgeDays),
                        icon = Icons.Default.Schedule
                    )
                }
            }
            
            if (!isOutliers) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricChip(
                        label = stringResource(R.string.memory_cluster_density, cluster.density),
                        icon = Icons.Default.CompareArrows
                    )
                    
                    MetricChip(
                        label = stringResource(R.string.memory_cluster_diversity, cluster.diversity),
                        icon = Icons.Default.Shuffle
                    )
                }
            }
            
            // Expanded memories list
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                HorizontalDivider()
                
                Spacer(modifier = Modifier.height(12.dp))
                
                cluster.memories.forEach { memoryWithAge ->
                    MemoryItem(memoryWithAge = memoryWithAge)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MemoryItem(memoryWithAge: MemoryWithAge) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            memoryWithAge.ageDays > 60 -> "🔴"
                            memoryWithAge.ageDays > 30 -> "🟡"
                            else -> "🟢"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "${memoryWithAge.ageDays}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = dateFormat.format(Date(memoryWithAge.memory.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = memoryWithAge.memory.fact,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


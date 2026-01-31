package com.yourown.ai.presentation.chat.components

/**
 * ChatDialogs.kt - Main entry point for all chat dialogs
 * 
 * This file re-exports all dialog functions from the dialogs package for convenience.
 * The actual implementations are organized into separate files:
 * 
 * - ConversationDialogs.kt: EditTitleDialog, RequestLogsDialog
 * - SearchDialogs.kt: SearchDialog, SystemPromptDialog
 * - ImportExportDialogs.kt: ExportChatDialog, ImportChatDialog, SourceChatSelectionDialog
 * - ErrorDialog.kt: ErrorDialog
 */

// Re-export all dialog functions for backward compatibility
// Import statements will be resolved by the IDE automatically

// From ConversationDialogs.kt
import com.yourown.ai.presentation.chat.components.dialogs.EditTitleDialog
import com.yourown.ai.presentation.chat.components.dialogs.RequestLogsDialog

// From SearchDialogs.kt
import com.yourown.ai.presentation.chat.components.dialogs.SearchDialog
import com.yourown.ai.presentation.chat.components.dialogs.SystemPromptDialog

// From ImportExportDialogs.kt
import com.yourown.ai.presentation.chat.components.dialogs.ExportChatDialog
import com.yourown.ai.presentation.chat.components.dialogs.ImportChatDialog
import com.yourown.ai.presentation.chat.components.dialogs.SourceChatSelectionDialog

// From ErrorDialog.kt
import com.yourown.ai.presentation.chat.components.dialogs.ErrorDialog

/**
 * All dialog functions are now imported from their respective files.
 * This file serves as a convenient single import point for consumers.
 * 
 * Example usage:
 * ```kotlin
 * import com.yourown.ai.presentation.chat.components.EditTitleDialog
 * import com.yourown.ai.presentation.chat.components.SearchDialog
 * // or
 * import com.yourown.ai.presentation.chat.components.dialogs.*
 * ```
 */

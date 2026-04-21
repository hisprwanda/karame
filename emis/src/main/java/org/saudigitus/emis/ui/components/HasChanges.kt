package org.saudigitus.emis.ui.components

import androidx.fragment.app.FragmentManager
import org.dhis2.commons.R
import org.dhis2.commons.dialogs.bottomsheet.BottomSheetDialog
import org.dhis2.commons.dialogs.bottomsheet.BottomSheetDialogUiModel
import org.dhis2.commons.dialogs.bottomsheet.DialogButtonStyle

fun launchBottomSheet(
    title: String,
    subtitle: String,
    supportFragmentManager: FragmentManager,
    onDiscard: () -> Unit, // Perform the transaction change and clear data
    onKeepEdition: () -> Unit, // Leave it as it was
) {
    BottomSheetDialog(
        bottomSheetDialogUiModel = BottomSheetDialogUiModel(
            title = title,
            message = subtitle,
            iconResource = R.drawable.ic_outline_error_36,
            mainButton = DialogButtonStyle.MainButton(R.string.keep_editing),
            secondaryButton = DialogButtonStyle.DiscardButton(),
        ),
        onMainButtonClicked = {
            supportFragmentManager.popBackStack()
            onKeepEdition.invoke()
        },
        onSecondaryButtonClicked = { onDiscard.invoke() },
    ).apply {
        this.show(supportFragmentManager.beginTransaction(), "DIALOG")
        this.isCancelable = false
    }
}
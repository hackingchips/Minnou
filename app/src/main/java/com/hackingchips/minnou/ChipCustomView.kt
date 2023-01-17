package com.hackingchips.minnou

import android.content.Context
import android.content.res.Resources
import android.view.ViewGroup
import androidx.core.view.isVisible

/**
 * A "ChipCustomView" object is created for every chip.
 *
 * @param context
 * @param chipSize              Size of the chip in grid boxes.
 * @param chipInGridPositionX   X position of the chip in the game board
 * @param chipInGridPositionY   Y position of the chip in the game board
 * @param chipOrientation       Orientation of the chip
 * @param scaleWidth            Scale to apply depending on screen size
 * @param scaleHeight           Scale to apply depending on screen size
 *
 */

class ChipCustomView(context: Context, var chipSize: Int, var chipInGridPositionX: Int, var chipInGridPositionY: Int, var chipOrientation: ChipOrientation, var scaleWidth: Float, var scaleHeight: Float):
    androidx.appcompat.widget.AppCompatImageView(context) {

    private var gridInDisplayPositionX: Float = 0f
    private var gridInDisplayPositionY: Float = 0f

    val Int.dp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()
    val Int.px: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    init {
        this.scaleType = ScaleType.FIT_XY
    }

    /**
     * Sets the initial position of a chip.
     *
     * @param gridPositionX         X position of the grid in the screen
     * @param gridPositionY         Y position of the grid in the screen
     * @param chipInGridPositionX   X position of the chip in the game board
     * @param chipInGridPositionY   Y position of the chip in the game board
     * @param chipOrientation       Orientation of the chip
     *
     */

    fun setChipPosition(gridPositionX: Float, gridPositionY: Float, chipInGridPositionX: Int, chipInGridPositionY: Int, chipOrientation: ChipOrientation) {

        gridInDisplayPositionX = gridPositionX
        gridInDisplayPositionY = gridPositionY

        this.chipOrientation = chipOrientation
        this.chipInGridPositionX = chipInGridPositionX
        this.chipInGridPositionY = chipInGridPositionY

        if (this.chipOrientation == ChipOrientation.HORIZONTAL) {

            val newLayoutParams = ViewGroup.LayoutParams((200 * chipSize * scaleWidth).toInt(), (200 * scaleHeight).toInt())
            this.layoutParams = newLayoutParams

            this.x = ((gridInDisplayPositionX + (50 * scaleWidth)) + (((this.chipInGridPositionX - 1) * 200) * scaleWidth)).toFloat()
            this.y = (((gridInDisplayPositionY + (50 * scaleHeight)) + ((this.chipInGridPositionY - 1) * 200 * scaleHeight))).toFloat()

            this.isVisible = true
        }
        else if (this.chipOrientation == ChipOrientation.VERTICAL) {

            val newLayoutParams = ViewGroup.LayoutParams((200 * scaleWidth).toInt(), (200 * chipSize * scaleHeight).toInt())
            this.layoutParams = newLayoutParams

            this.x = ((gridInDisplayPositionX + (50 * scaleWidth)) + (((this.chipInGridPositionX - 1) * 200) * scaleWidth)).toFloat()
            this.y = (((gridInDisplayPositionY + (50 * scaleHeight)) + ((this.chipInGridPositionY - 1) * 200 * scaleHeight))).toFloat()

            this.isVisible = true
        }
        else if (this.chipOrientation == ChipOrientation.NONE) {
            this.isVisible = false
        }
    }

    /**
     * Moves the chip to a new position.
     *
     * @param boardNewPositionX   X position of the chip in the game board
     * @param boardNewPositionY   Y position of the chip in the game board
     *
     */

    fun updateChipPosition(boardNewPositionX: Int, boardNewPositionY: Int) {

        setChipPosition(gridInDisplayPositionX, gridInDisplayPositionY, boardNewPositionX, boardNewPositionY, this.chipOrientation)
    }

}
package com.hackingchips.minnou

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.Resources.getSystem
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.abs


class MainActivity : AppCompatActivity() {

    private var gridX: Float = 0f
    private var gridY: Float = 0f

    // *** Screen dimensions and scaling data.

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private var scaleWidth: Float = 0f
    private var scaleHeight: Float = 0f

    // *** Refs. of UI elements.

    private lateinit var buttonDecrease: Button
    private lateinit var buttonIncrease: Button
    private lateinit var buttonStartStop: Button
    private lateinit var buttonReset: Button
    private lateinit var labelLevel: TextView
    private lateinit var labelTime: TextView
    private lateinit var labelMoves: TextView

    private lateinit var redChipAnimator: ObjectAnimator

    private var touchDownX: Int = 0
    private var touchDownY: Int = 0

    // *** Game data.

    private var chipsData: ChipsData = ChipsData()                                          // *** Definition of every chip
                                                                                            //     and its size.
    private var theGameLevels = TheGameLevels()                                             // *** Data about position of chips
                                                                                            //     in every level.

    private  var chipsCollection: MutableMap<String, ChipCustomView> = mutableMapOf()       // *** Collection of chips

    private var gameBoardPositions = arrayOf(
        arrayOf("0", "0", "0", "0", "0", "0"),
        arrayOf("0", "0", "0", "0", "0", "0"),
        arrayOf("0", "0", "0", "0", "0", "0"),
        arrayOf("0", "0", "0", "0", "0", "0"),
        arrayOf("0", "0", "0", "0", "0", "0"),
        arrayOf("0", "0", "0", "0", "0", "0")
    )

    private var gameLevel: Int = 1
    private var levelMoves: Int = 0

    private var gameRunning: Boolean = false

    private lateinit var gameTimer: Timer
    private var seconds: Int = 0
    private var minutes: Int = 0
    private var hours: Int = 0

    // *** Sounds

    private lateinit var chipMoveSound: MediaPlayer
    private lateinit var levelEndSound: MediaPlayer

    // ********************************************************************************************

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            val defaultDisplay =
                DisplayManagerCompat.getInstance(this).getDisplay(Display.DEFAULT_DISPLAY)
            val displayContext = createDisplayContext(defaultDisplay!!)

            screenWidth = displayContext.resources.displayMetrics.widthPixels
            screenHeight = displayContext.resources.displayMetrics.heightPixels

        } else {

            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)

            screenWidth = displayMetrics.heightPixels
            screenHeight = displayMetrics.widthPixels

        }

        // *** Store refs. for UI elements.

        buttonDecrease = findViewById(R.id.ibButtonDecreaseLevel)
        buttonIncrease = findViewById(R.id.ibButtonIncreaseLevel)
        buttonStartStop = findViewById(R.id.ibButtonStart)
        buttonReset = findViewById(R.id.ibButtonReset)
        labelLevel = findViewById(R.id.tvLevelValue)
        labelTime = findViewById(R.id.tvTime)
        labelMoves = findViewById(R.id.tvMoves)

        // *** Initialize labels.

        labelLevel.text = gameLevel.toString()
        labelLevel.invalidate()
        labelMoves.text = getString(R.string.moves_label_text) + " $levelMoves"
        labelMoves.invalidate()

        // *** Initialize buttons events.

        buttonStartStop.setOnClickListener{onStartStopButtonClick()}
        buttonReset.setOnClickListener{onResetButtonClick()}
        buttonDecrease.setOnClickListener{onDecreaseButtonClick()}
        buttonIncrease.setOnClickListener{onIncreaseButtonClick()}

        // *** Initialize the game after the main view has been layed out.

        val mv = findViewById<RelativeLayout>(R.id.viewLayout)
        mv.doOnNextLayout { initGame() }

    }

    private fun initGame() {

        val gameGrid: ImageView = findViewById(R.id.gameGrid)
        val gridWidth = gameGrid.width
        val gridHeight = gameGrid.height

        // *** Get coordinates of where the system draws the grid.
        gridX = gameGrid.x
        gridY = gameGrid.y

        // *** Calculate scaling coeficientes based on dimensions of the
        //     original design of the grid.
        //     It was designed with dimensions 1330x1300 pixels.
        scaleWidth = gridWidth.toFloat() / 1300f                               // Calculate coefficients.
        scaleHeight = gridHeight.toFloat() / 1300f                             // for scaling.

        // *** Create the chips.

        for (chip in chipsData.chips) {

            val chipcustomview = ChipCustomView(this, chip.value, 0, 0, ChipOrientation.NONE, scaleWidth, scaleHeight)

            chipcustomview.tag = chip.key
            chipcustomview.x = 46f
            chipcustomview.y = 371f
            chipcustomview.setImageResource(resources.getIdentifier(chip.key.toString().lowercase(), "drawable", this.packageName))
            chipcustomview.isVisible = true

            chipcustomview.setOnTouchListener{ v, event -> onChipTouched(v, event)}

            chipsCollection[chip.key] = chipcustomview

            findViewById<ConstraintLayout>(R.id.llFrame).addView(chipcustomview)
        }

        // *** Initialize position of every chip.
        setChipsPositionForLevel()

        // *** Sounds
        chipMoveSound = MediaPlayer.create(this, R.raw.chipmove)
        chipMoveSound.isLooping = false
        levelEndSound = MediaPlayer.create(this, R.raw.levelend)
        levelEndSound.isLooping = false
    }

    /**
     * Initialize every position of the array used to store the game state
     * to "0"
     */
    private fun initBoardPositions() {
        for (row in gameBoardPositions.indices) {
            for (column in gameBoardPositions[row].indices) {
                gameBoardPositions[row][column] = "0" //just an example
            }
        }
    }

    /**
     * Places each chip at its corresponding position depending on actual game
     * level.
     */
    private fun setChipsPositionForLevel()
    {
        // *** First clear the boar.
        initBoardPositions()

        for (chip in theGameLevels.gameLevels[gameLevel - 1]) {

            chipsCollection[chip.key]?.setChipPosition(gridX, gridY,
                chip.value[0].toString().toInt(), chip.value[1].toString().toInt(), chip.value[2] as ChipOrientation)

            if (chip.value[2] == ChipOrientation.HORIZONTAL)
                for (counter: Int in 0 until chipsCollection[chip.key]?.chipSize!!)
                    gameBoardPositions[(chip.value[1].toString().toInt() - 1)][(chip.value[0].toString().toInt() - 1 + counter)] = chip.key
            else if (chip.value[2] == ChipOrientation.VERTICAL)
                for (counter: Int in 0 until chipsCollection[chip.key]?.chipSize!!)
                    gameBoardPositions[(chip.value[1].toString().toInt() - 1 + counter)][(chip.value[0].toString().toInt() - 1)] = chip.key
        }
    }

    /***
     * This function is called after every move to check if the user finished
     * the current level.
     * If yes, then the red chip is animated and moved outside the grid.
     */
    private fun checkWinning() {

        val ivChipXPosition = chipsCollection["ivChipX"]?.chipInGridPositionX?.minus(1)

        if (ivChipXPosition != null) {

            for (winposition in (ivChipXPosition + 2)..5) {

                if (gameBoardPositions[2][winposition] != "0")
                    return
            }

            // *** Play end of level sound.
            levelEndSound.start()

            // Stop the timer.
            gameTimer.cancel()
            gameTimer.purge()

            // *** Disable all actions during the red chip animation.
            gameRunning = false

            /* *** Create the animator for the red chip.
               When a level is won, the animator moves the red chip outside of the board.
               During animation, buttons response is discarded.
               At the end of animation, the function "endOfLevel" is called to increase the level
               if possible and restore buttons response.
        */
            redChipAnimator = ObjectAnimator.ofFloat(chipsCollection["ivChipX"], "translationX", screenWidth + 1000f)
            redChipAnimator.duration = 2000
            redChipAnimator.doOnEnd { endOfLevel() }
            redChipAnimator.start()
        }
    }

    /**
     * Called at the end of the red chip animation.
     */
    private fun endOfLevel() {

        // *** Increase the level if posible.

        if (gameLevel < 60) {

            gameLevel++

            labelLevel.text = gameLevel.toString()
            labelLevel.invalidate()

            setChipsPositionForLevel()
        }

        // *** Buttons Start/Stop and Reset are adjusted at its initial
        //     state. The user has to start the new level manually.
        buttonIncrease.setBackgroundResource(R.drawable.button_background)
        buttonDecrease.setBackgroundResource(R.drawable.button_background)
        buttonStartStop.setBackgroundResource(R.drawable.button_start)
        buttonStartStop.text = getString(R.string.start_text)
        buttonReset.setBackgroundResource(R.drawable.button_disabled)

        // *** Restore all functions.
        gameRunning = false
    }

    /**
     * Move a chip if posible.
     */
    private fun moveChip(theChip: String, theMove: String) {

        // *** Get actual coordinates of the chip in the grid.
        //     And its size.
        val chipx = chipsCollection[theChip]?.chipInGridPositionX
        val chipy = chipsCollection[theChip]?.chipInGridPositionY
        val chipsize = chipsCollection[theChip]?.chipSize

        when (chipsCollection[theChip]?.chipOrientation) {

            ChipOrientation.HORIZONTAL -> {
                /* *** Check if next position is valid and is empty. *** */
                when (theMove)
                {
                    "left" -> {
                        if (chipx != null) {
                            if (chipx > 1) {
                                if(gameBoardPositions[chipy!! - 1][chipx - 1 -1] == "0") {
                                    gameBoardPositions[chipy!! - 1][chipx -1 -1 + chipsize!!] = "0"
                                    gameBoardPositions[chipy!! - 1][chipx -1 -1 ] = theChip
                                    chipsCollection[theChip]?.updateChipPosition(chipx - 1, chipy)

                                    levelMoves++
                                    labelMoves.text = getString(R.string.moves_label_text) + " $levelMoves"
                                    labelMoves.invalidate()

                                    // *** Play chip move sound
                                    chipMoveSound.start()

                                    checkWinning()
                                }
                            }
                        }
                    }

                    "right" -> {
                        if (chipx != null) {
                            if ((chipx + chipsize!!) <= 6) {
                                if(gameBoardPositions[chipy!! - 1][chipx + chipsize - 1] == "0") {
                                    gameBoardPositions[chipy!! - 1][chipx - 1] = "0"
                                    gameBoardPositions[chipy!! - 1][chipx -1  + chipsize!!] = theChip
                                    chipsCollection[theChip]?.updateChipPosition(chipx + 1, chipy)

                                    levelMoves++
                                    labelMoves.text = getString(R.string.moves_label_text) + " $levelMoves"
                                    labelMoves.invalidate()

                                    // *** Play chip move sound
                                    chipMoveSound.start()

                                    checkWinning()
                                }
                            }
                        }
                    }
                }
            }

            ChipOrientation.VERTICAL -> {
                when (theMove)
                {
                    "up" -> {
                        if (chipy != null) {
                            if (chipy > 1) {
                                if(gameBoardPositions[chipy!! - 1 -1][chipx!! - 1 ] == "0") {
                                    gameBoardPositions[chipy!! - 1 -1 + chipsize!!][chipx!! - 1] = "0"
                                    gameBoardPositions[chipy!! - 1 -1][chipx - 1] = theChip
                                    chipsCollection[theChip]?.updateChipPosition(chipx!!, chipy - 1)

                                    levelMoves++
                                    labelMoves.text = getString(R.string.moves_label_text) + " $levelMoves"
                                    labelMoves.invalidate()

                                    // *** Play chip move sound
                                    chipMoveSound.start()

                                    checkWinning()
                                }
                            }
                        }
                    }

                    "down" -> {
                        if (chipy != null) {
                            if ((chipy + chipsize!!) <= 6) {
                                if(gameBoardPositions[chipy!! - 1 + chipsize][chipx!! - 1] == "0") {
                                    gameBoardPositions[chipy!! - 1][chipx - 1] = "0"
                                    gameBoardPositions[chipy!! - 1  + chipsize!!][chipx!! - 1] = theChip
                                    chipsCollection[theChip]?.updateChipPosition(chipx, chipy + 1)

                                    levelMoves++
                                    labelMoves.text = getString(R.string.moves_label_text) + " $levelMoves"
                                    labelMoves.invalidate()

                                    // *** Play chip move sound
                                    chipMoveSound.start()

                                    checkWinning()
                                }
                            }
                        }
                    }
                }
            }

            else -> {}
        }

        for (row in gameBoardPositions.indices) {
            for (column in gameBoardPositions[row].indices)
                print(gameBoardPositions[row][column].padStart(10, ' '))
            print("\n")
        }

        print("\n\n")
    }

    /**
     * Detect a touch on some chip.
     * Detect direction of the move.ç
     * Only allowed to move chips if a level is being played.
     */
    private fun onChipTouched(chip: View, event: MotionEvent): Boolean {

        if (gameRunning) {

            // *** Get the touched chip.
            val thechip = chip.tag.toString()

            when(event.action){

                // *** If touch started, then store actual coordinates.
                MotionEvent.ACTION_DOWN -> {
                    touchDownX = event.x.toInt()
                    touchDownY = event.y.toInt()
                }

                // *** If touch finished, then calculate horizontal and vertical
                //     vertical move from previous stored coordinates.
                MotionEvent.ACTION_UP -> {
                    val deltaX = touchDownX - event.x.toInt()
                    val deltaY = touchDownY - event.y.toInt()
                    var movement = ""

                    // *** Check if movement is horizontal or vertical
                    if (abs(deltaX) > abs(deltaY)) {                        // *** Horizontal.

                        // Check if movement is to the right or the left.
                        if (deltaX < 0) movement = "right"
                        else movement = "left"
                    }
                    else {                                                  // *** Vertical.

                        // Check if movement is to the top or bottom
                        if (deltaY < 0) movement = "down"
                        else movement = "up"
                    }

                    // *** Move the chip (if posible)
                    moveChip(thechip, movement)
                }

                MotionEvent.ACTION_MOVE -> { }
                MotionEvent.ACTION_CANCEL -> { }
                else -> { }
            }
        }

        return true
    }

    /* *******************************************************************************************

        BUTTONS EVENTS

     ******************************************************************************************* */

    /**
     * Decrease level if possible.
     * Only allowed if the game is stoped.
     */
    private fun onDecreaseButtonClick() {

        if (!gameRunning) {

            if (gameLevel > 1) {

                gameLevel--
                labelLevel.text = gameLevel.toString()
                labelLevel.invalidate()

                levelMoves = 0
                labelMoves.text = getString(R.string.moves_label_text) + " $levelMoves"
                labelMoves.invalidate()

                setChipsPositionForLevel()
            }
        }
    }

    /**
     * Increase level if possible.
     * Only allowed if the game is stoped.
     */
    private fun onIncreaseButtonClick() {

        if (!gameRunning) {

            if (gameLevel < 60) {

                gameLevel++
                labelLevel.text = gameLevel.toString()
                labelLevel.invalidate()

                levelMoves = 0
                labelMoves.text = getString(R.string.moves_label_text) + " $levelMoves"
                labelMoves.invalidate()

                setChipsPositionForLevel()
            }
        }
    }

    /**
     * Start/Stop button event.
     * Starts or stops a level.
     */
    private fun onStartStopButtonClick() {

        if (!gameRunning) {

            // *** Change text of the Start/Stop button.
            //     Disable Increase and Decrease buttons
            //     Enable the "Level Reset" button
            //     Initialize labels.
            buttonIncrease.setBackgroundResource(R.drawable.button_disabled)
            buttonDecrease.setBackgroundResource(R.drawable.button_disabled)
            buttonStartStop.setBackgroundResource(R.drawable.button_stop)
            buttonStartStop.text = getString(R.string.stop_text)
            buttonReset.setBackgroundResource(R.drawable.button_background)

            levelMoves = 0
            labelMoves.text = getString(R.string.moves_label_text) + " $levelMoves"
            labelMoves.invalidate()

            // *** Initialize and start the timer.
            hours = 0
            seconds = 0
            minutes = 0

            gameTimer = timer(name = "timer-1", initialDelay = 0, period = 1000) {
                this@MainActivity.runOnUiThread {
                    seconds++

                    if (seconds == 60) {
                        seconds = 0
                        minutes++
                    }

                    if (minutes == 60) {
                        minutes = 0
                        hours++
                    }

                    if (hours == 100)
                        hours = 0

                    (getString(R.string.time_label_text) + " " + hours.toString().padStart(2, '0') + ":" +
                            minutes.toString().padStart(2, '0') + ":" + seconds.toString().padStart(2, '0')).also { labelTime.text = it }
                    labelTime.invalidate()
                }
            }

            gameRunning = true
        }
        else {

            // *** Change text of the Start/Stop button.
            //     Enable Increase and Decrease buttons
            //     Disable the "Level Reset" button
            buttonIncrease.setBackgroundResource(R.drawable.button_background)
            buttonDecrease.setBackgroundResource(R.drawable.button_background)
            buttonStartStop.setBackgroundResource(R.drawable.button_start)
            buttonStartStop.text = getString(R.string.start_text)
            buttonReset.setBackgroundResource(R.drawable.button_disabled)

            gameRunning = false

            gameTimer.cancel()
            gameTimer.purge()
        }

    }

    /**
     * Reset position of the chips for the current level.
     * Only allowed if a level is being played
     *
     * This does not initializes time and moves counter.
     */
    private fun onResetButtonClick() {

        if (gameRunning)
            setChipsPositionForLevel()
    }



}


package dev.nenoeldeeb.education.absencerecord.data.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import dev.nenoeldeeb.education.absencerecord.R
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

object CalendarImageGenerator {
    private const val CALENDAR_WIDTH = 1080
    private const val CALENDAR_HEIGHT = 1920
    private const val PADDING = 50f
    private const val BACKGROUND_COLOR = "#FF202124"
    private const val TITLE_TEXT_SIZE = 70f
    private const val MONTH_TEXT_SIZE = 50f
    private const val DAY_HEADER_TEXT_SIZE = 35f
    private const val DAY_TEXT_SIZE = 40f
    private const val CELL_HEIGHT = 150f
    private const val HIGHLIGHT_COLOR = "#7781C784"
    private const val CIRCLE_RADIUS_FRACTION = 1f / 3f

    /**
     * Creates a calendar bitmap for a given month with marked attendance days.
     *
     * @param context Context to retrieve string resources
     * @param month The month and year for the calendar (must be set to the 1st of the month)
     * @param studentName The name of the student to display on the calendar
     * @param markedDays Set of day numbers (1-31) that should be highlighted
     * @return A bitmap containing the generated calendar
     */
    fun generateCalendarBitmap(
        context: Context,
        month: LocalDate,
        studentName: String,
        markedDays: Set<Int>
    ): Bitmap {
        val contentWidth = CALENDAR_WIDTH - 2 * PADDING
        val bitmap = createBitmap(CALENDAR_WIDTH, CALENDAR_HEIGHT)
        val canvas = Canvas(bitmap)

        // Draw background
        canvas.drawColor(BACKGROUND_COLOR.toColorInt())

        // Draw title (student name)
        val titlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = TITLE_TEXT_SIZE
                textAlign = Paint.Align.CENTER
            }
        canvas.drawText(studentName, CALENDAR_WIDTH / 2f, PADDING + TITLE_TEXT_SIZE, titlePaint)

        // Draw month and year
        val monthPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.LTGRAY
                textSize = MONTH_TEXT_SIZE
                textAlign = Paint.Align.CENTER
            }
        val monthYPos = PADDING + 160f

        val monthName = context.getString(getMonthResId(month.month))
        val monthYearString = context.getString(R.string.date_format_month_year, monthName, month.year)

        canvas.drawText(
            monthYearString,
            CALENDAR_WIDTH / 2f,
            monthYPos,
            monthPaint
        )

        // Draw calendar grid
        drawCalendarGrid(
            context,
            canvas,
            month,
            markedDays,
            contentWidth,
            PADDING,
            monthYPos + 80f
        )

        return bitmap
    }

    /**
     * Draws the calendar grid on the canvas.
     */
    private fun drawCalendarGrid(
        context: Context,
        canvas: Canvas,
        month: LocalDate,
        markedDays: Set<Int>,
        gridWidth: Float,
        startX: Float,
        startY: Float
    ) {
        val dayHeaderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = DAY_HEADER_TEXT_SIZE
                textAlign = Paint.Align.CENTER
            }
        val dayPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = DAY_TEXT_SIZE
                textAlign = Paint.Align.CENTER
            }
        val highlightPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = HIGHLIGHT_COLOR.toColorInt()
                style = Paint.Style.FILL
            }

        val cellWidth = gridWidth / 7f
        val textOffsetY = (CELL_HEIGHT / 2) - ((dayPaint.descent() + dayPaint.ascent()) / 2)

        // Draw day headers (Saturday to Friday)
        val daysOfWeek =
            (0..6).map { i ->
                // Calculate DayOfWeek starting from Saturday
                // = 6, Sunday = 7, Monday = 1...
                val isoDay = (DayOfWeek.SATURDAY.isoDayNumber - 1 + i) % 7 + 1
                DayOfWeek(isoDay)
            }

        val headerY = startY + 40f
        daysOfWeek.forEachIndexed { index, dayOfWeek ->
            val dayStr = context.getString(getDayOfWeekShortResId(dayOfWeek))
            canvas.drawText(
                dayStr,
                startX + cellWidth * index + cellWidth / 2,
                headerY,
                dayHeaderPaint
            )
        }

        val gridStartY = headerY + 80f

        val nextMonth = month.plus(1, DateTimeUnit.MONTH)
        val lastDayOfMonth = nextMonth.minus(1, DateTimeUnit.DAY)
        val totalDaysInMonth = lastDayOfMonth.day

        var firstDayCalendarOffset = month.dayOfWeek.isoDayNumber - DayOfWeek.SATURDAY.isoDayNumber
        if (firstDayCalendarOffset < 0) firstDayCalendarOffset += 7

        // Draw each day in the calendar grid
        (1..totalDaysInMonth).forEach { day ->
            val x = startX + (firstDayCalendarOffset + day - 1) % 7 * cellWidth
            val y = gridStartY + (firstDayCalendarOffset + day - 1) / 7 * CELL_HEIGHT

            // Highlight the background for marked days
            if (markedDays.contains(day)) {
                val circleRadius = cellWidth * CIRCLE_RADIUS_FRACTION
                canvas.drawCircle(
                    x + cellWidth / 2,
                    y + CELL_HEIGHT / 2,
                    circleRadius,
                    highlightPaint
                )
            }

            // Draw the day number
            canvas.drawText(
                day.toString(),
                x + cellWidth / 2,
                y + textOffsetY,
                dayPaint
            )
        }
    }

    private fun getMonthResId(month: Month): Int {
        return when (month) {
            Month.JANUARY -> R.string.month_january
            Month.FEBRUARY -> R.string.month_february
            Month.MARCH -> R.string.month_march
            Month.APRIL -> R.string.month_april
            Month.MAY -> R.string.month_may
            Month.JUNE -> R.string.month_june
            Month.JULY -> R.string.month_july
            Month.AUGUST -> R.string.month_august
            Month.SEPTEMBER -> R.string.month_september
            Month.OCTOBER -> R.string.month_october
            Month.NOVEMBER -> R.string.month_november
            Month.DECEMBER -> R.string.month_december
        }
    }

    private fun getDayOfWeekShortResId(dayOfWeek: DayOfWeek): Int {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> R.string.day_short_monday
            DayOfWeek.TUESDAY -> R.string.day_short_tuesday
            DayOfWeek.WEDNESDAY -> R.string.day_short_wednesday
            DayOfWeek.THURSDAY -> R.string.day_short_thursday
            DayOfWeek.FRIDAY -> R.string.day_short_friday
            DayOfWeek.SATURDAY -> R.string.day_short_saturday
            DayOfWeek.SUNDAY -> R.string.day_short_sunday
        }
    }
}
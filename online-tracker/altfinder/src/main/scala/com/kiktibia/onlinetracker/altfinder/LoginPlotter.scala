package com.kiktibia.onlinetracker.altfinder

import com.kiktibia.onlinetracker.altfinder.repo.Model.OnlineDateSegment
import org.knowm.xchart.HeatMapChartBuilder
import scala.jdk.CollectionConverters.*
import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.style.Styler.ChartTheme
import java.awt.Color
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object LoginPlotter {

  def plot(logins: List[OnlineDateSegment]) = {
    val chart = new HeatMapChartBuilder().title("title here").theme(ChartTheme.Matlab).build()

    val characterNames = logins.map(_.characterName).distinct
    val (heatData, times) = loginsToHeatData(logins, characterNames, Duration.ofMinutes(1))
    val nLabels = 33
    val parsedTimes = times.zipWithIndex.map { case (t, i) =>
      if (i % (times.length / nLabels) == 0) t.format(DateTimeFormatter.ofPattern("HH:mm")) else " "
    }
    chart.addSeries("series name", parsedTimes.asJava, characterNames.asJava, heatData.asJava)
    chart.getStyler().setRangeColors(Array(Color.WHITE, Color.GREEN))
    new SwingWrapper[HeatMapChart](chart).displayChart()
  }

  private def loginsToHeatData(
      logins: List[OnlineDateSegment],
      characterNames: List[String],
      duration: Duration
  ): (List[Array[Number]], List[OffsetDateTime]) = {

    val latest = logins.map(_.end).max

    @annotation.tailrec
    def go(
        l: List[Array[Number]],
        times: List[OffsetDateTime],
        currentTime: OffsetDateTime,
        n: Int
    ): (List[Array[Number]], List[OffsetDateTime]) = {
      val nextTime = currentTime.plus(duration)
      val onlineChars = logins.filter { s =>
        (s.start.isBefore(nextTime) || s.start.isEqual(nextTime)) &&
        (currentTime.isBefore(s.end) || currentTime.isEqual(s.end))
      }
      val onlineHeatEntries = onlineChars.map { c =>
        val i = characterNames.indexOf(c.characterName)
        Array[Number](n, i, 1)
      }
      val offlineHeatEntries = characterNames.diff(onlineChars).map { c =>
        val i = characterNames.indexOf(c)
        Array[Number](n, i, 0)
      }
      if (nextTime.isAfter(latest)) (l.reverse, times.reverse)
      else go(onlineHeatEntries ::: offlineHeatEntries ::: l, currentTime :: times, nextTime, n + 1)
    }
    go(Nil, Nil, logins.head.start, 0)
  }
}

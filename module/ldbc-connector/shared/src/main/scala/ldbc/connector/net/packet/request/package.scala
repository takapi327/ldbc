/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*

package object request:
  
  def time: Encoder[java.time.LocalTime] = Encoder { localTime =>
    for
      hour   <- uint8.encode(localTime.getHour)
      minute <- uint8.encode(localTime.getMinute)
      second <- uint8.encode(localTime.getSecond)
    yield hour ++ minute ++ second
  }
  
  def date: Encoder[java.time.LocalDate] = Encoder { localDate =>
    for
      year  <- uint16.encode(localDate.getYear)
      month <- uint8.encode(localDate.getMonthValue)
      day   <- uint8.encode(localDate.getDayOfMonth)
    yield year ++ month ++ day
  }
  
  def dateTime: Encoder[java.time.LocalDateTime] = Encoder { localDateTime =>
    for
      date <- date.encode(localDateTime.toLocalDate)
      time <- time.encode(localDateTime.toLocalTime)
    yield date ++ time
  }

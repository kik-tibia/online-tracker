package com.kiktibia.onlinetracker.tracker.tibiadata.response

case class Api(version: Int, release: String, commit: String)

case class Status(http_code: Int)

case class Information(api: Api, timestamp: String, status: Status)

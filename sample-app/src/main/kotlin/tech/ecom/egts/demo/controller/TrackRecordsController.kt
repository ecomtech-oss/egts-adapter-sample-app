package tech.ecom.egts.demo.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.ecom.egts.demo.model.CourierTrackRecord
import tech.ecom.egts.demo.model.random
import tech.ecom.egts.demo.service.TrackRecordsSender

@RestController
@RequestMapping("/track")
class TrackRecordsController(
    private val trackRecordsSender: TrackRecordsSender,
) {

    @PostMapping("/send_track_record")
    fun sendTrackCourierRecord(@RequestBody trackRecordsToSend: Int) =
        trackRecordsSender.sendTrackRecords(
            List(trackRecordsToSend) { CourierTrackRecord.random() },
        )

}
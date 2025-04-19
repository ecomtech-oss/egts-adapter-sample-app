package tech.ecom.egts.demo.extension.model.sfrd.record.subrecord.types

import tech.ecom.egts.library.model.sfrd.record.subrecord.SubRecordData

data class CounterSubRecordData(
    val counterNumber: Byte,
    val counterValue: Int,
) : SubRecordData()
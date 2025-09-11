package com.example.the_machine.dto.content

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@SerialName(ContentBlockTypes.FILE_REF)
data class FileRefDto(
  val assetId: @Contextual UUID,
  val filename: String,
  val mime: String,
  val size: Long
) : ContentBlockDto
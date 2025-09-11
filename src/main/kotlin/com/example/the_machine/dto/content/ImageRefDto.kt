package com.example.the_machine.dto.content

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@SerialName(ContentBlockTypes.IMAGE_REF)
data class ImageRefDto(
  val assetId: @Contextual UUID
) : ContentBlockDto
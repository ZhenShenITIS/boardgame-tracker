package itis.boardgametracker.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "s3")
data class S3Properties (
    val baseS3Url: String
){
}
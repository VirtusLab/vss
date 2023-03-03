package com.virtuslab.vss.common

import org.apache.commons.codec.digest.DigestUtils

import java.security.MessageDigest

object HashAlgorithm:
  def hash(original: String): String =
    DigestUtils.sha256Hex(original)

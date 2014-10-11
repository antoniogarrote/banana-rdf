package org.w3.banana
package io

import java.io._

import scala.concurrent.Future
import scala.util._

trait RDFReader[Rdf <: RDF, +S] {

  /**
   * legacy: if one passes an input stream at this layer one
   * would need to know the encoding too. This function is badly designed.
   * @param is
   * @param base
   * @return
   */
  @deprecated
  def read(is: InputStream, base: String): Try[Rdf#Graph]

  //todo: this is for rdfstorew that uses a lot more callbacks than Jena or Sesame
  //it would probably be better to have RDFReader[M: Monad, Rdf<: RDF, +S] and the return be M[Rdf#Graph]
  def read(is: String, base: String): Future[Rdf#Graph] = {
    //this would be the scala 2.11 version
    // Future.fromTry(read(new ByteArrayInputStream(is.getBytes("UTF-8")),base))
    read(new ByteArrayInputStream(is.getBytes("UTF-8")), base) match {
      case Success(s) => Future.successful(s)
      case Failure(f) => Future.failed(f)
    }

  }

}

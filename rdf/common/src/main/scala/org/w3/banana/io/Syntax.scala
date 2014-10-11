package org.w3.banana
package io

import scalaz.NonEmptyList

/* some well-known mime-types so that we can refer to them in banana-rdf */

trait N3
trait Turtle
trait RDFXML
trait RDFaXHTML
trait SparqlAnswerJson
trait SparqlAnswerXml

trait JsonLd
trait JsonLdCompacted
trait JsonLdExpanded
trait JsonLdFlattened

/**
 * typeclass for a Syntax
 * It must say the mime-types that are associated to it
 */
trait SyntaxMime[+T, MimeType] {

  /**
   * the mime-types for this syntax
   *
   * Per convention, the first one is the default one
   */
  def mimeTypes: NonEmptyList[MimeType]

  /**
   * The default mime type to use for this syntax. Usually published at the IETF in their
   * <a href="http://www.iana.org/assignments/media-types/index.html">mime type registry</a>.
   */
  def standardMimeType: MimeType = mimeTypes.head

}

trait Syntax[+T] extends SyntaxMime[T,MimeType]


/**
 * some Syntax instances for the well-known mime-types
 */
object Syntax {

  def apply[T](implicit syntax: Syntax[T]): Syntax[T] = syntax

  implicit val RDFQueryLang: Syntax[RDF#Query] = new Syntax[RDF#Query] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(MimeType("application", "sparql-query"))
  }

  implicit val N3: Syntax[N3] = new Syntax[N3] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(MimeType("text", "n3"), MimeType("text", "rdf+n3"))
  }

  implicit val Turtle: Syntax[Turtle] = new Syntax[Turtle] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(MimeType("text", "turtle"))
  }

  implicit val RDFXML: Syntax[RDFXML] = new Syntax[RDFXML] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(MimeType("application", "rdf+xml"))
  }

  implicit val JsonLdCompacted: Syntax[JsonLdCompacted] = new Syntax[JsonLdCompacted] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(
      MimeType("application", "ld+json", Map("profile" -> "http://www.w3.org/ns/json-ld#compacted"))
    )
  }

  implicit val JsonLdExpanded: Syntax[JsonLdExpanded] = new Syntax[JsonLdExpanded] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(
      MimeType("application", "ld+json", Map("profile" -> "http://www.w3.org/ns/json-ld#expanded"))
    )
  }

  implicit val JsonLdFlattened: Syntax[JsonLdFlattened] = new Syntax[JsonLdFlattened] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(
      MimeType("application", "ld+json", Map("profile" -> "http://www.w3.org/ns/json-ld#flattened"))
    )
  }

  implicit val JSON_LD: Syntax[JsonLd] = new Syntax[JsonLd] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(MimeType("application", "ld+json"))
  }

  implicit val RDFaXHTML: Syntax[RDFaXHTML] = new Syntax[RDFaXHTML] {
    val mimeTypes: NonEmptyList[MimeType] = NonEmptyList(MimeType("text", "html"), MimeType("application", "xhtml+xml"))
  }

  implicit val SparqlAnswerJson = new Syntax[SparqlAnswerJson] {
    val mimeTypes = NonEmptyList(MimeType("application", "sparql-results+json"))
  }

  implicit val SparqlAnswerXml = new Syntax[SparqlAnswerXml] {
    val mimeTypes = NonEmptyList(MimeType("application", "sparql-results+xml"))
  }

  implicit val SparqlUpdate = new Syntax[RDF#UpdateQuery] {
    val mimeTypes = NonEmptyList(MimeType("application", "sparql-update"))
  }

  implicit val textPlain = new Syntax[String] {
    val mimeTypes = NonEmptyList(MimeType("text", "plain"))
  }

}

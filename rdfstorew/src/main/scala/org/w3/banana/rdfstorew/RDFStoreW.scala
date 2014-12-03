package org.w3.banana.rdfstorew

import org.w3.banana.{RDFStore, RDFOps, SparqlUpdate}

import scala.concurrent._
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic.global
import scala.util.Try

class RDFStoreW()(implicit ops: RDFOps[JSStore]) extends RDFStore[JSStore, Future, js.Dynamic] with SparqlUpdate[JSStore, Future, js.Dynamic] {

  def executeQuery(store: js.Dynamic, sparql: String): Future[Any] = {
    val promise = Promise[Any]

    store.applyDynamic("execute")(sparql, { (success: Boolean, res: js.Any) =>
      if (success) {
        promise.success(res)
      } else {
        promise.failure(new Exception("Error running query: " + res))
      }
    })

    promise.future
  }

  def executeQueryUnit(store: js.Dynamic, sparql: String): Future[Unit] = {
    val promise = Promise[Unit]

    store.applyDynamic("execute")(sparql, { (success: Boolean, res: js.Any) =>
      if (success) {
        promise.success()
      } else {
        promise.failure(new Exception("Error running query: " + res))
      }
    })

    promise.future
  }

  def bindQuery(query: String, bindings: Map[String, RDFStoreRDFNode]): String = {
    var tmp = query
    for ((name, node) <- bindings) {
      tmp = tmp.replaceAll("?" + name, node.jsNode.toNT().asInstanceOf[js.String])
    }
    tmp
  }


  /* Transactor */

  // RDFStore-js doesn't support transactions

  /** Evaluates `body` in a read transaction. */
  override def r[T](store: js.Dynamic, body: => T): Try[T] = Try {
    try {
      val result = body
      result
    }
  }

  /** Evaluates `body` in a read/write transaction. */
  override def rw[T](store: js.Dynamic, body: => T): Try[T] = Try {
    try {
      val result = body
      result
    }
  }


  /** Executes a Construct query. */
  override def executeConstruct(store: js.Dynamic, query: JSStore#ConstructQuery, bindings: Map[String, JSStore#Node]): Future[JSStore#Graph] = {
    executeQuery(store, bindQuery(query, bindings)) map {
      g => {
        new RDFStoreGraph(g.asInstanceOf[js.Dynamic])
      }
    }
  }

  /** Executes a Select query. */
  override def executeSelect(store: js.Dynamic, query: JSStore#SelectQuery, bindings: Map[String, JSStore#Node]): Future[JSStore#Solutions] = {
    executeQuery(store, bindQuery(query, bindings)) map {
      solutions =>
        solutions.asInstanceOf[js.Array[js.Dynamic]].map[SPARQLSolutionTuple] {
          (o: js.Dynamic) => new SPARQLSolutionTuple(o.asInstanceOf[js.Dictionary[js.Any]])
        }.toArray
    }
  }

  /** Executes a Ask query. */
  override def executeAsk(store: js.Dynamic, query: JSStore#AskQuery, bindings: Map[String, JSStore#Node]): Future[Boolean] = {
    executeQuery(store, bindQuery(query, bindings)) map {
      b => {
        b.asInstanceOf[Boolean]
      }
    }
  }

  override def executeUpdate(store: js.Dynamic, query: JSStore#UpdateQuery, bindings: Map[String, JSStore#Node]): Future[js.Dynamic] = {
    executeQueryUnit(store, bindQuery(query, bindings)) map identity
    Future.successful(store)
  }

  /**
   * To the graph at `uri`, removes the matching triples
   */
  override def removeTriples(store: js.Dynamic, graph: JSStore#URI, triples: Iterable[(JSStore#NodeMatch, JSStore#NodeMatch, JSStore#NodeMatch)]): Future[Unit] = {
    val promise = Promise[Unit]
    val cb = {
      (success: Boolean, res: Any) =>
        if (success) {
          promise.success()
        } else {
          promise.failure(new Exception("Error deleting triples into the store: " + res))
        }
    }

    val graphToRemove = ops.emptyGraph
    for(triple <- triples) {
      triple match {
        case (PlainNode(s), PlainNode(p:RDFStoreNamedNode), PlainNode(o)) =>
          val triple = ops.makeTriple(s,p,o)
          graphToRemove.add(triple)
      }
    }


      if (graph == null) {
        store.applyDynamic("delete")(graphToRemove.graph, cb)
      } else {
        store.applyDynamic("delete")(graphToRemove.graph, graph.valueOf, cb)
      }

    promise.future
  }

  /**
   * To the graph at `uri`, appends the content of `graph`. If there was
   * no previous graph, this would create it.
   */
  override def appendToGraph(store: js.Dynamic, graph: JSStore#URI, triples: JSStore#Graph): Future[Unit] = {
    val promise = Promise[Unit]
    val cb = {
      (success: Boolean, res: Any) =>
        if (success) {
          promise.success()
        } else {
          promise.failure(new Exception("Error inserting triples into the store: " + res))
        }
    }

    if (graph == null) {
      store.applyDynamic("insert")(triples.graph, cb)
    } else {
      store.applyDynamic("insert")(triples.graph, graph.valueOf, cb)
    }

    promise.future
  }

  /** Removes the graph at `uri`. */
  override def removeGraph(store: js.Dynamic, graph: JSStore#URI): Future[Unit] = {
    val promise = Promise[Unit]
    val cb = {
      (success: Boolean, res: Any) =>
        if (success) {
          promise.success()
        } else {
          promise.failure(new Exception("Error cleaning graph from the store store: " + res))
        }
    }

    if (graph == null) {
      store.applyDynamic("clear")(cb)
    } else {
      store.applyDynamic("clear")(graph.valueOf, cb)
    }

    promise.future
  }

  /** Gets the graph at `uri`. */
  override def getGraph(store: js.Dynamic, uri: JSStore#URI): Future[JSStore#Graph] = {
    val promise = Promise[RDFStoreGraph]
    val cb = {
      (success: Boolean, res: js.Dynamic) =>
        if (success) {
          promise.success(new RDFStoreGraph(res))
        } else {
          promise.failure(new Exception("Error exporting data as a RDF graph"))
        }
    }
    store.applyDynamic("graph")(uri.valueOf, cb)
    promise.future
  }

}

object RDFStoreW {

  var rdfstorejs:js.Dynamic = makeRDFStoreJS(Map())

  val rdf = rdfstorejs.selectDynamic("rdf")


  val rdf_api = rdfstorejs.selectDynamic("rdf").selectDynamic("api")

  def apply(options: Map[String, Any]): RDFStoreW = {
    rdfstorejs = makeRDFStoreJS(options)
    new RDFStoreW()(JSStore.ops)
  }

  def makeRDFStoreJS(options: Map[String, Any]):js.Dynamic = {
    val dic = options.foldLeft[js.Dictionary[Any]](js.Dictionary())({
      case (acc, (key, value)) =>
        acc.update(key, value); acc
    })

    val promise = Promise[Unit]

    // hack for Rhino/browser execution
    val rdfstore = if (global.window != null) {
      global.window.rdfstore
    } else {
      global.rdfstore
    }
    var newRdfstorejs:js.Dynamic = null
    rdfstore.applyDynamic("create")(dic, (store: js.Dynamic) => promise.success{
      newRdfstorejs = store
    })

    // always succeeds because 'create' is synchronous
    promise.future.value.get.get

    newRdfstorejs
  }
}
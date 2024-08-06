package chainless.utils

import cats.collections.Heap
import cats.implicits.*
import fs2.{Pull, Stream}

// https://gist.github.com/johnynek/689199b4ac49364e7c94abef996ae59f
object SortMergeStream {
  def apply[F[_], A: Ordering](streams: List[Stream[F, A]]): Stream[F, A] = {
    implicit val ord: cats.Order[Stream.StepLeg[F, A]] =
      new cats.Order[Stream.StepLeg[F, A]] {
        private val ordA = implicitly[Ordering[A]]

        def compare(left: Stream.StepLeg[F, A], right: Stream.StepLeg[F, A]): Int = {
          if (left.head.isEmpty) {
            // prefer to step so we don't skip items
            if (right.head.isEmpty) 0 else -1
          } else if (right.head.isEmpty) {
            // we need to step so we don't misorder items
            1
          } else {
            // neither are empty just compare the head
            ordA.compare(left.head(0), right.head(0))
          }
        }
      }

    def go(heap: Heap[Stream.StepLeg[F, A]]): Pull[F, A, Unit] =
      heap.pop match {
        case Some((sl, rest)) =>
          if (sl.head.nonEmpty) {
            Pull.output1(sl.head(0)) >> {
              val nextSl = sl.setHead(sl.head.drop(1))
              val nextHeap = rest.add(nextSl)
              go(nextHeap)
            }
          } else {
            // this chunk is done
            sl.stepLeg
              .flatMap {
                case Some(nextSl) =>
                  val nextHeap = rest.add(nextSl)
                  go(nextHeap)
                case None =>
                  // this leg is exhausted
                  go(rest)
              }
          }

        case None => Pull.done
      }

    def heapOf(ls: List[Stream.StepLeg[F, A]]): Heap[Stream.StepLeg[F, A]] =
      Heap.fromIterable(ls)

    val heap: Pull[F, Nothing, Heap[Stream.StepLeg[F, A]]] =
      streams
        .traverse(_.pull.stepLeg)
        .map { ls => heapOf(ls.flatten) }

    heap.flatMap(go).stream
  }
}

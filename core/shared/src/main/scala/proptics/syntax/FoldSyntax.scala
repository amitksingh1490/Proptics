package proptics.syntax

import scala.reflect.ClassTag

import cats.Monoid

import proptics.{Fold, Fold_}

trait FoldSyntax {
  implicit def foldOnSyntax[S](s: S): FoldOnSyntax[S] = FoldOnSyntax(s)
}

final case class FoldOnSyntax[S](private val s: S) extends AnyVal {
  import proptics.syntax.fold._

  def viewOn[A: Monoid](fold: Fold[S, A]): A = fold.view(s)

  def viewAllOn[A](fold: Fold[S, A]): List[A] = s.toListOn(fold)

  def previewOn[A: Monoid](fold: Fold[S, A]): Option[A] = fold.preview(s)

  def toListOn[A](fold: Fold[S, A]): List[A] = fold.toList(s)

  def toArrayOn[T, A: ClassTag, B](fold: Fold_[S, T, A, B]): Array[A] = fold.toArray(s)
}
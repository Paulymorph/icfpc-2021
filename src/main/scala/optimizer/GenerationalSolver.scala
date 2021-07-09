package icfpc21.classified
package optimizer

import icfpc21.classified.model.{Figure, Hole, Problem, Solution}
import icfpc21.classified.optimizer.mutators._
import icfpc21.classified.solver.{Solver, SolverListener}

import scala.util.Random

class GenerationalSolver(solverListener: SolverListener) extends Solver {
  val count = 500
  val ChildrenPerGeneration = 5
  val MutationsPerChild = 5
  val GenerationsCount = 500

  val mutators: Seq[Mutator] = Seq(
    MirrorMutator,
    MovePointMutator,
    IdentityMutator,
    SmallMovePointMutator,
    SmallMovePointMutator,
    MoveOutsidePointMutator,
    MoveOutsidePointMutator,
    MoveOutsidePointMutator,
  )

  def generate(figure: Figure, hole: Hole): Seq[Figure] = {
    (0 until ChildrenPerGeneration).map { _ =>
      (0 until MutationsPerChild).foldLeft(figure) { (f, _) =>
        val mIdx = Random.nextInt(mutators.size)
        val m = mutators(mIdx)
        m.mutate(f, hole, speed = 1d)
      }
    } ++ Seq(figure)
  }

  override def solve(problem: Problem): Solution = {
    def printScore(generation: Int, best: Figure): Unit = {
      println(
        s"## Generation $generation: Best score: ${Scorer.score(best, problem)}, " +
          s"fits: ${Scorer.checkFits(best, problem.hole)}, " +
          s"valid: ${Scorer.checkStretchingIsOk(best, problem)}, " +
          s"outside: ${Scorer.scoreOutsidePoints(best, problem.hole)}, " +
          s"dislikes: ${Scorer.scoreDislikes(best, problem.hole)}           ### " + Solution(best.vertices)
      )
    }

    def isFinished(best: Figure): Boolean = {
      Scorer.checkFits(best, problem.hole) &&
      Scorer.checkStretchingIsOk(best, problem) &&
      Scorer.scoreDislikes(best, problem.hole) == 0d
    }
    printScore(0, problem.figure)

    var candidates = Seq.fill(count)(problem.figure)
    var generation = 0
    var finished = false
    while (generation < GenerationsCount && !finished) {
      generation += 1
      val newGeneration = candidates.flatMap(generate(_, problem.hole))
      val sorted = newGeneration.sortBy(f => Scorer.score(f, problem))
      val selected = sorted.takeRight(count)
      solverListener.candidates(selected.takeRight(5))
      printScore(generation, selected.last)
      finished = isFinished(selected.last)

      candidates = selected
    }

    val result = Solution(candidates.last.vertices)
    solverListener.solution(result)
    result
  }
}

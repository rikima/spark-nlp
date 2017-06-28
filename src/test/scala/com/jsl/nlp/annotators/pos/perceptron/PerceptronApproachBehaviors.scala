package com.jsl.nlp.annotators.pos.perceptron

import com.jsl.nlp.annotators.common.{TaggedSentence, TokenizedSentence}
import com.jsl.nlp.{Annotation, AnnotatorBuilder, Document}
import org.apache.spark.sql.{Dataset, Row}
import org.scalatest._

import scala.collection.mutable.{Set => MSet}

/**
  * Created by Saif Addin on 5/18/2017.
  */
trait PerceptronApproachBehaviors { this: FlatSpec =>

  def isolatedPerceptronTraining(trainingSentences: Array[TaggedSentence]): Unit = {
    s"Average Perceptron tagger" should "successfully train a provided wsj corpus" in {
      val nIterations = 5
      val tagger = PerceptronApproach.train(trainingSentences, nIterations)
      val model = tagger.model
      val nWords = trainingSentences.map(_.words.length).sum
      assert(
        nWords * nIterations == model.getUpdateIterations,
        s"because Words: $nWords -- nIterations: $nIterations -- multip: ${nWords * nIterations}" +
          s"-- model iters: ${model.getUpdateIterations}"
      )
      val tagSet: MSet[String] = MSet()
      trainingSentences.foreach{s => {
        s.tags.foreach(tagSet.add)
      }}
      assert(tagSet.size == model.getTags.length)
      tagSet.foreach(tag => assert(model.getTags.contains(tag)))
    }
  }

  def isolatedPerceptronTagging(
                                 trainedTagger: PerceptronApproach,
                                 targetSentences: Array[TokenizedSentence]
                               ): Unit = {
    s"Average Perceptron tagger" should "successfully tag all word sentences after training" in {
      val result = trainedTagger.tag(targetSentences)
      assert(result.head.words.length == targetSentences.head.tokens.length, "because tagger returned less than" +
        " the amount of appropriate tagged words")
    }
  }

  def isolatedPerceptronTagCheck(
                                trainedTagger: PerceptronApproach,
                                targetSentence: Array[TokenizedSentence],
                                expectedTags: Array[String]
                                ): Unit = {
    s"Average Perceptron tagger" should "successfully return expected tags" in {
      val resultTags = trainedTagger.tag(targetSentence).head
      val resultContent = resultTags.taggedWords.zip(expectedTags)
        .filter(rte => rte._1.tag != rte._2)
        .map(rte => (rte._1.word, (rte._1.tag, rte._2)))
      assert(resultTags.words.length == expectedTags.length, s"because tag amount ${resultTags.words.length} differs from" +
        s" expected ${expectedTags.length}")
      assert(resultTags.taggedWords.zip(expectedTags).forall(t => t._1.tag == t._2), s"because expected tags do not match returned" +
        s" tags.\n------------------------\n(word,(result,expected))\n-----------------------\n${resultContent.mkString("\n")}")
    }
  }

  def sparkBasedPOSTagger(dataset: => Dataset[Row]): Unit = {
    "a Perceptron POS tagger Annotator" should s"successfully tag sentences " in {
      println(dataset.schema)
      AnnotatorBuilder.withFullPOSTagger(dataset)
        .collect().foreach {
        row =>
          val document = Document(row.getAs[Row](0))
          println(document)
          row.getSeq[Row](2).map(Annotation(_)).foreach {
            matchedAnnotation =>
              println(matchedAnnotation)
          }
      }
    }
  }

}

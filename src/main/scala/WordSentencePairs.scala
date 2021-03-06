package multip;

import multip.feature._
import scala.io.Source
import scala.collection.mutable.ArrayBuffer
import Array._

import java.util.regex.Pattern

import edu.washington.cs.knowitall.morpha._

import breeze.linalg._
import breeze.numerics._

/** Class RawWordPair:
*    a class that extracts features for a word pair (iword1 and iword2) of sentence pair (rawspair)
*/
class RawWordPair (iword1:Int, iword2:Int, rawspair:RawSentencePair) {

	var rawfeatures:Array[String] = new Array[String](0)

	val len1 = rawspair.owords.length
	val len2 = rawspair.cwords.length


	val word1 = rawspair.owords(iword1)
	val word2 = rawspair.cwords(iword2)
	val pos1 = StringSim.specialPOS(word1, rawspair.oposs(iword1))
	val pos2 = StringSim.specialPOS(word2, rawspair.cposs(iword2))
	val stem1 = rawspair.ostems(iword1)
	val stem2 = rawspair.cstems(iword2)

	val npos1 = if(pos1.length() >= 2) {pos1.substring(0,2)} else {pos1}
	val npos2 = if(pos2.length() >= 2) {pos2.substring(0,2)} else {pos2}


	val nword1 = rawspair.onwords(iword1)
	val nword2 = rawspair.cnwords(iword2)
	val nstem1 = rawspair.onstems(iword1)
	val nstem2 = rawspair.cnstems(iword2)

	val lexicalized = false


	this.extract_Unlexicalized_Complex_Features()

	def extract_Unlexicalized_Complex_Features() {

		val wordfeatures = this.extract_Word_Features()
		val posfeatures = this.extract_POS_Features()
		val word_posfeatures = this.combineFeatures(wordfeatures, posfeatures)

		//val sig_features = this.extract_WordSig_Features()
		//val word_sigfeatures = this.combineFeatures(wordfeatures, sig_features)
		//val word_pos_sigfeatures = this.combineFeatures(word_posfeatures, sig_features)

		this.rawfeatures = concat (this.rawfeatures, word_posfeatures)
		//this.rawfeatures = concat (this.rawfeatures, word_sigfeatures)
		//this.rawfeatures = concat (this.rawfeatures, word_pos_sigfeatures)

	}

	def combineFeatures (fset1:Array[String], fset2:Array[String]) : Array[String] = {
		val rawfeaturesbuffer = new ArrayBuffer[String]()

		for (f1 <- fset1) {
			for (f2 <- fset2) {
				val combinedfeature = f1 + "_" + f2
				rawfeaturesbuffer += combinedfeature
			}
		}

		return rawfeaturesbuffer.toArray
	}

	def extract_Word_Features(): Array[String] = {
		val rawfeaturesbuffer = new ArrayBuffer[String]()
		if (word1 == word2) {
			rawfeaturesbuffer += "sameword"
		} else if (stem1 == stem2) {
			rawfeaturesbuffer += "samestem"
		} else if (nword1 == nword2) {
			rawfeaturesbuffer += "samenword"
		} else if (nstem1 == nstem2) {
			rawfeaturesbuffer += "samenstem"
		} else if (StringSim.similarWords(word1, word2)) {
			rawfeaturesbuffer += "similarword"
		} else {
			rawfeaturesbuffer += "diffword"
		}

		return rawfeaturesbuffer.toArray
	}


	def extract_POS_Features(): Array[String] = {
		val rawfeaturesbuffer = new ArrayBuffer[String]()
		if (npos1 == npos2){
			rawfeaturesbuffer += "samepos" + npos1.toLowerCase()
		} else if (word1 == word2) {
			rawfeaturesbuffer += "diffpossameword"
		} else {
			rawfeaturesbuffer += "singlepos" + npos1.toLowerCase()
			rawfeaturesbuffer += "singlepos" + npos2.toLowerCase()

			rawfeaturesbuffer += "diffpos"
		}

		return rawfeaturesbuffer.toArray
	}


	def extract_WordSig_Features(): Array[String] = {
		val rawfeaturesbuffer = new ArrayBuffer[String]()

		val trendid = this.rawspair.trendid

		val sig1 = WordSig.getWordSiginTrend(word1, trendid)
		val sig2 = WordSig.getWordSiginTrend(word2, trendid)

		if (sig1 > 10000.0 && sig2 > 5000.0) {
			rawfeaturesbuffer += "bothsupersig"
		} else if (sig1 > 1000.0 && sig2 > 1000.0) {
			rawfeaturesbuffer += "bothverysig"
		} else if (sig1 > 500.0 && sig2 > 500.0) {
			rawfeaturesbuffer += "bothsig"
		} else {
			rawfeaturesbuffer += "notbothsig"
		}

		return rawfeaturesbuffer.toArray
	}

	override def toString() :String = {

		var output = word1 + " (" + stem1 + ") | " + word2 + " (" + stem2 + ")"
		output += " |"
		for (rfeature <- this.rawfeatures) {
			output += " "+ rfeature
		}
		return output
	}

}

abstract class SuperRawSentencePair  {
	val rawwordpairs:Array[RawWordPair]
	val rawfeatureset:Set[String]

	val origsent:String
	val candsent:String

	val trendid:String
	val trendname:String

	val label: Option[Boolean]
	def isParaphrase: Boolean = label.getOrElse(false)
	//val amtjudge:Option[Boolean]
	//val expertjudge:Option[Boolean]

	val owords:Array[String]
	val cwords:Array[String]
	val oposs:Array[String]
	val cposs:Array[String]
	val ostems:Array[String]
	val cstems:Array[String]

	val onwords:Array[String]
	val cnwords:Array[String]
	val onstems:Array[String]
	val cnstems:Array[String]

	val valid:Boolean //both sentences match the topic names
}

object RawSentencePair {

	val ENGLISH_STOPWORDS = {
		Set("i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom", "this", "that", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of", "at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "should", "now")
	}

	val BIOMEDICAL_STOPWORDS = {
		Set(".", ":", ",", "?", ";", "!", "``", "&", "''", "...", "-", "(", ")", "amp", "study", "'s", "new", "de", "science", "research", "paper", "health", "nature", "article", "la", "%", "en", "risk", "'", "|", "n't", "use", "read", "human", "cancer", "may", "interesting", "social", "brain", "evidence", "”", "el", "“", "data", "us", "mt", "disease", "scientists", "review", "good")
	}

	/**
	* Find unique common subparts of defined maxLength between two sentences of words.
	* The sentences' words can also be empty, in which case they are not considered.
	*
	* Examples:
	*
	* * [A] & [A, A] -> Set([A])
	* * [A, B, C] & [X, B, C] -> Set([B], [C], [BC])
	* * [A,  , C] & [X, B, C] -> Set([C])
	*
	*/
	def findCommonSubparts(s1: Array[String], s2: Array[String], maxLength: Int = 2, stopWords: Set[String] = ENGLISH_STOPWORDS ++ BIOMEDICAL_STOPWORDS): Set[Array[String]] = {
		assert(maxLength >= 1, "maxLength must be possitive")
		/* Possible addition: allow for own set/string equality function */

		/* Is this more efficent to compute as a variant of the Smith-Waterman algorithm ? */
		def subparts(s: Array[String], size: Int, index: Int = 0, aux: Set[List[String]] = Set()): Set[List[String]] = {
			if (index + size - 1 == s.size) aux.filter(!_.exists(_.isEmpty))
			else {
				subparts(s, size, index + 1, aux + s.slice(index, index + size).toList)
			}
		}

		val ret = (1 to maxLength).foldLeft(Set[List[String]]()) { (ret, size) =>
			ret ++ (subparts(s1, size) & subparts(s2, size))
		}.map(_.toArray)

		ret.filterNot(subpart => stopWords.exists(st => subpart.indexOf(st) != -1))
	}

	def getBestCommonSubpart(
		commons: Iterable[Array[String]],
		isFirstBetter: (Array[String], Array[String]) => Boolean = ((a, b) => (a.mkString("").length >= b.mkString("").length))
	): Array[String] = {

		if (commons.isEmpty) Array()
		else {
			commons.tail.foldLeft(commons.head) { (prev, neu)  =>
				if (isFirstBetter(prev, neu)) prev else neu
			}
		}
	}

}

/** Class RawSentencePair:
*    A data structure describe a sentence pair with all original information in String
*    format that is used to read-in original data from (text format) annotation file and
*    then covert to features into class SentPairsData.
*/
class RawSentencePair (val trendid:String, val trendname:String, val origpossent:String, val candpossent:String, val label: Option[Boolean]) extends SuperRawSentencePair {
	import RawSentencePair._

	// create words/poss/stems arrays from input sentences
	val otmptags = origpossent.split(" ")
	val ctmptags = candpossent.split(" ")
	val ocasewords = otmptags.map(x => x.split('/')(0))
	val ccasewords = ctmptags.map(x => x.split('/')(0))
	val otmpwords = ocasewords.map(x => x.toLowerCase())
	val ctmpwords = ccasewords.map(x => x.toLowerCase())
	val otmpposs = otmptags.map(x => x.split('/')(2))
	val ctmpposs = ctmptags.map(x => x.split('/')(2))

	private val okTrendPos = Set("cc", "cd", "fw", "jj", "jjs", "ls", "md", "nn", "nns", "nnp", "nnps", "prp", "sym", "vbd", "vbg", "vbn", "vbp", "vbz", "wdt", "wp", "wp$", "wrb") //best
	def filterWordsByPos(s: Array[String], spos: Array[String]): Array[String] = {
		var i = -1
		s.map { w => i += 1; if (okTrendPos.contains(spos(i))) w else "" }
	}

	val trendnamewords = {
		if (trendname.isEmpty) {
			getBestCommonSubpart(findCommonSubparts(filterWordsByPos(otmpwords, otmpposs), filterWordsByPos(ctmpwords, ctmpposs)))
		}
		else trendname.replace("-","").toLowerCase().split(' ')
	}

	/* Typical behavior (original MultiP algorithm) is to disregard the trending words as possible features.
	* CAUTION However, we need them on very short sentences, with equal size of the trending words vector. Otherwise, there would be no features
	* This in fact invalidates original MultiP's algorithm that paraphrases must at least have trending words & an anchor in common */
	val disregardTrendingWords = trendnamewords.size < otmpwords.size && trendnamewords.size < ctmpwords.size

	val origsent = ocasewords.mkString(" ")
	val candsent = ccasewords.mkString(" ")

	def readTokens(tmpwords: Array[String], tmpposs: Array[String], disregardTrendingWords: Boolean = true) = {
		var words_buffer = new ArrayBuffer[String]()
		var poss_buffer = new ArrayBuffer[String]()

		var index = 0
		var retMatched = false
		while (index < tmpwords.length) {
			var matched = false
			if (trendnamewords.nonEmpty && disregardTrendingWords) {
				var i = 0
				if (tmpwords(index+i) == trendnamewords(i)) {
					matched = true
					i += 1
					while (matched && index + i < tmpwords.length && i < trendnamewords.length) {
						if (tmpwords(index+i) != trendnamewords(i)) {
							matched = false
						}
						i += 1
					}
				}
			}

			if (matched == true) {
				retMatched = true
				words_buffer += "XXXX"
				poss_buffer  += "XX"
				index += trendnamewords.length
			} else {
				words_buffer += tmpwords(index)
				poss_buffer  += tmpposs(index)
				index += 1
			}
		}
		(retMatched, words_buffer.toArray, poss_buffer.toArray)
	}

	val (omatched, owords, oposs) = readTokens(otmpwords, otmpposs, disregardTrendingWords)
	val (cmatched, cwords, cposs) = readTokens(ctmpwords, ctmpposs, disregardTrendingWords)

	val followsig = WordSig.getWordSiginTrend("follow", this.trendid)

	val valid = (omatched == true && cmatched == true && origsent != candsent && followsig <= 50000.0)

	val ostems = owords map {MorphaStemmer.stemToken(_)}
	val cstems = cwords map {MorphaStemmer.stemToken(_)}

	val onwords = owords map {StringSim.normalizedByDictionary(_)}
	val cnwords = cwords map {StringSim.normalizedByDictionary(_)}
	val onstems = onwords map {MorphaStemmer.stemToken(_)}
	val cnstems = cnwords map {MorphaStemmer.stemToken(_)}


	// extract word pairs
	val rawwordpairs = this.extractWordPairsTrick()

	//End of the constructor of RawSentencePair

	var rawfeatureset_buffer = Set.empty[String]
	for (wpair <- this.rawwordpairs) {
		rawfeatureset_buffer = wpair.rawfeatures.toSet ++ rawfeatureset_buffer
	}
	val rawfeatureset = rawfeatureset_buffer

	//Align the words on each side that share the same stem together
	//for other words, use all possible word pairs
	def extractWordPairsTrick () : Array[RawWordPair] = {
		var wordpairs_buffer:ArrayBuffer[RawWordPair] = new ArrayBuffer[RawWordPair]()

		val stemincommon = this.ostems.toSet & this.cstems.toSet

		for (ioword <- 0 until owords.length) {
			val ostem = this.ostems(ioword)

			for (icword <- 0 until cwords.length) {
				val cstem = this.cstems(icword)

				if ((ostem == cstem && stemincommon(ostem) && stemincommon(cstem)) && ostem!="XXXX" || (!stemincommon(ostem) && !stemincommon(cstem))) {
					val wpair = new RawWordPair(ioword, icword, this)
					wordpairs_buffer += wpair
				}
			}
		}

		return wordpairs_buffer.toArray
	}

	override def toString(): String = {
		var output = this.label + " | " + this.trendname + " | " + this.origsent + " | " + this.candsent + "\n"
		output += this.owords.mkString(" ") + " | " + this.ostems.mkString(" ") + " | " + this.oposs.mkString(" ") + "\n"
		output += this.cwords.mkString(" ") + " | " + this.cstems.mkString(" ") + " | " + this.cposs.mkString(" ")
		for (rwpair <- this.rawwordpairs) {
			output += "\n" + rwpair.toString()
		}

		output += "\n"
		return output
	}

}


abstract class SuperVectorSentencePair  {

	val origsent:String
	val candsent:String

	val trendid:String
	val trendname:String

	val label: Option[Boolean]
	def isParaphrase: Boolean = label.getOrElse(false)

	val nRel = 2  // number of relations is 2, either paraphrase or not paraphrase.

	val IS_PARAPHRASE = 1
	val IS_NOT_PARAPHRASE = 0

	var w1ids:Array[Int] = null
	var w2ids:Array[Int] = null
	var features:Array[SparseVector[Double]] = null
	var rel:Transpose[DenseVector[Double]] = DenseVector.zeros[Double](nRel).t
	var z:DenseVector[Int] = null // Though it is Int, but only will have two possible values 0 or 1
	var zScore:DenseVector[Double] = null

	var postZ:DenseMatrix[Double] = null

}

/** Class VectorSentencePair:
*   A data structure that describes a sentence pair using compact Vector format
*   and only carries a few important information in String format
*   it can be generated from RawSentencePair
*/
class VectorSentencePair (val trendid:String, val trendname:String, val origsent:String, val candsent:String, val label: Option[Boolean]) extends SuperVectorSentencePair {

	def this(rawsentpair: RawSentencePair) {
		this(rawsentpair.trendid, rawsentpair.trendname, rawsentpair.origsent, rawsentpair.candsent, rawsentpair.label)
	}

	def this(rawsentpair: RawSentencePair, w1ids: Array[Int], w2ids: Array[Int], features: Array[SparseVector[Double]]) {
		this(rawsentpair.trendid, rawsentpair.trendname, rawsentpair.origsent, rawsentpair.candsent, rawsentpair.label)

		this.w1ids = w1ids
		this.w2ids = w2ids
		this.features = features

		if (this.label == Some(true)) {
			this.rel.t(IS_PARAPHRASE) = 1.0
		} else {
			this.rel.t(IS_NOT_PARAPHRASE) = 1.0
		}
	}

	def this(vsentpair: VectorSentencePair, w1ids: Array[Int], w2ids: Array[Int], features: Array[SparseVector[Double]], rel: Transpose[DenseVector[Double]], z: DenseVector[Int], zScore: DenseVector[Double]) {
		this(vsentpair.trendid, vsentpair.trendname, vsentpair.origsent, vsentpair.candsent, vsentpair.label)

		this.w1ids = w1ids
		this.w2ids = w2ids
		this.features = features
		this.rel = rel
		this.z = z
		this.zScore = zScore
	}


	override def toString() :String = {
		var output = "YesPara = " + this.rel(IS_PARAPHRASE) + " | " + "NonPara = " + this.rel(IS_NOT_PARAPHRASE) + " | " + this.trendname + " | " + this.origsent + " | " + this.candsent

		output += "\n"
		return output
	}

}

case class Data(training: SentPairsData, evaluation: SentPairsData) {

}

object Data {

	val USE_POS = true
	val NGRAM_PHRASE_PAIR = 1
	val N_FEATURE_CUTOFF = 3

	def createOne(rawSentPairsTraining: ArrayBuffer[RawSentencePair], rawSentPairsEvaluation: ArrayBuffer[RawSentencePair]): Data = {
		// read in training data, then test data.
	  // the order matters, since test data has to create the features that exist in
	  // the training data and use the same mapping to convert features into vector representations.

		val training = createSentPairsData(rawSentPairsTraining)
		val evaluation = createSentPairsData(rawSentPairsEvaluation, training.featureVocab)

		Data(training, evaluation)
	}

	def createCV(rawSentPairs: ArrayBuffer[RawSentencePair], numCV: Int = 10, randomOrder: Boolean = false): Traversable[Data] = {
		val in = (if (randomOrder) util.Random.shuffle(rawSentPairs) else rawSentPairs)
		val setSize = Math.ceil(rawSentPairs.size / numCV.toDouble).toInt

		(0 until numCV).toIterable.map(_ * setSize).map { evalStart =>
			val evalEnd = Math.min(evalStart + setSize, in.size)
			val train = new ArrayBuffer[RawSentencePair]()
			val eval = new ArrayBuffer[RawSentencePair]()
			for (i <- 0 until in.size) {
				if (i >= evalStart && i < evalEnd) eval += in(i)
				else train += in(i)
			}
			val training = createSentPairsData(train)
			val evaluation = createSentPairsData(eval, training.featureVocab)
			Data(training, evaluation)
		}
	}

	/**
	* When reading in training data, let the input parameter "featureVocab" as default,
	* and a new mapping of features to their vector index (featureVocab) will be built.
	* Next, when reading in the test data, point the input paramater "featureVocab" to
	* the trainingData.featureVocab instance you just created, and the feature vectors
	* for test data will be created, using the same feature set of the training data.
	*/
	private def createSentPairsData(rawSentPairs: ArrayBuffer[RawSentencePair], featureVocab: Vocab = new Vocab()): SentPairsData = {
		val sentVocab = new Vocab
		val wordVocab = new Vocab

		for (rsentpair <- rawSentPairs) {
			sentVocab.apply(rsentpair.origsent)
			sentVocab.apply(rsentpair.candsent)

			for (rwpair <- rsentpair.rawwordpairs) {
				wordVocab.apply(rwpair.word1)
				wordVocab.apply(rwpair.word2)
			}
		}

		if (!featureVocab.isLocked()) {
			//Add the features (count 1 for each sent pair)
			var rawfeaturecounter = Map.empty[String, Int]
			rawSentPairs.foreach { rsentpair =>
				rawfeaturecounter = rawfeaturecounter ++ rsentpair.rawfeatureset.zip(Stream.continually(1)).toMap.map{ case (k,v) => k -> (v + rawfeaturecounter.getOrElse(k,0)) }
			}

			//Filter out features that appear in less than N_FEATURE_CUTOFF sentence pairs
			for ((fstr, fcount) <- rawfeaturecounter) {
				if (fcount >= N_FEATURE_CUTOFF) {
					featureVocab.apply(fstr)
				}
			}

			featureVocab.lock()
		}

		val data = new Array[VectorSentencePair](rawSentPairs.size)
		for ((rspair, index) <- rawSentPairs.zipWithIndex) {
			val w1s = new Array[Int](rspair.rawwordpairs.length)
			val w2s = new Array[Int](rspair.rawwordpairs.length)
			val swfeatures = new Array[SparseVector[Double]](rspair.rawwordpairs.length)

			for (i <- 0 until rspair.rawwordpairs.length) {
				val wpair:RawWordPair = rspair.rawwordpairs(i)
				w1s(i) = wordVocab(rspair.rawwordpairs(i).word1)
				w2s(i) = wordVocab(rspair.rawwordpairs(i).word2)

				swfeatures(i) = SparseVector.zeros[Double](featureVocab.size + 1)
				swfeatures(i)(featureVocab.size) = 1.0	//Bias feature

				for(j <- 0 until wpair.rawfeatures.length) {
					val f = featureVocab(wpair.rawfeatures(j))
					//println(f + " " + ppair.rawfeatures(j))
					if (f >= 0) {
						swfeatures(i)(f) = 1.0
					}
				}
			}

			data(index) = new VectorSentencePair(rspair, w1s, w2s, swfeatures)
		}

		new SentPairsData(data, sentVocab.lock(), wordVocab.lock(), featureVocab)
	}

	/**
	* Parse something like (4, 1) to optional binary value Option[true/false]
	*/
	def parseAmazonTurkLabel(x: String): Option[Boolean] = {
		val AMT_JUDGE_HIGH_THRESHOLD = 3
		val AMT_JUDGE_LOW_THRESHOLD = 1

		var tmp_amtjudge:Option[Boolean] = None
		if (x.charAt(0) == '(') {
			val amttmp = x.charAt(1).asDigit
			if (amttmp >= AMT_JUDGE_HIGH_THRESHOLD) {
				tmp_amtjudge = Some(true)
			} else if (amttmp <= AMT_JUDGE_LOW_THRESHOLD) {
				tmp_amtjudge = Some(false)
			}
		}
		tmp_amtjudge
	}

	/**
	* Parse something like 4 to optional binary value Option[true/false]
	*/
	def parseExpertLabel(x: String): Option[Boolean] = {
		val EXPERT_JUDGE_HIGH_THRESHOLD = 4
		val EXPERT_JUDGE_LOW_THRESHOLD = 2

		var tmp_expertjudge:Option[Boolean] = None
		if (x != null) {
			val experttmp = x.toInt
			if (experttmp >= EXPERT_JUDGE_HIGH_THRESHOLD) {
				tmp_expertjudge = Some(true)
			} else if (experttmp <= EXPERT_JUDGE_LOW_THRESHOLD) {
				tmp_expertjudge = Some(false)
			}
		}
		tmp_expertjudge
	}

	def readPitFile(file: java.io.File, useExpert: Boolean, forTraining: Boolean): ArrayBuffer[RawSentencePair] = {
		var rawsentpairs = new ArrayBuffer[RawSentencePair]()

		println("Read In Data From Annotation File: " + file)

		var nLines = 0
		for (line <- Source.fromFile(file).getLines()) {
			nLines += 1
			if (nLines % 1000 == 0) {
				println("    read " + nLines + " lines")
			}

			val cols = line.toLowerCase().trim().split('\t')
			var rsentpair: RawSentencePair = null

			//Read In one sentence pair from original annotation file
			if (USE_POS) {
				val isTest = {
					if (cols.length == 8) true // 8-column format: both Amazon Mechanical Turk and Expert label
					else { assert(cols.length == 7, s"${nLines}: ${line}"); false } // 7-column format: only Amazon Mechanical Turk label
				}

				// Note!! the "test" prefix before the trend it (cols(0)) is hard-coded for purpose of
				// reading in the topic-word significance for the Topical features only.
				// The Topical features were used in our TACL paper, and computed by an external script (not in MultiP)
				// and saved in the data files, which are read into the MultiP.
				// The "test" prefix is to distinguish the different trend id indices used for training and test data

				// If you want to remove this topical feature (also called Sig feature in the code of MultiP),
				// you would want to modify the code of class RawWordPair in this file.

				rsentpair = new RawSentencePair(
					trendid = (if (isTest) "test" else "") + cols(0),
					trendname = cols(1),
					origpossent = if (isTest) cols(6) else cols(5),
					candpossent = if (isTest) cols(7) else cols(6),
					label = if (isTest && useExpert) parseExpertLabel(cols(5)) else parseAmazonTurkLabel(cols(4)))
				}

				//Extract phrase pairs and their features for this sentence pair

				//CAUTION Original code had always the check: rsentpair.valid -- Disregard the check for testing to report on all instances
				if (rsentpair != null && rsentpair.label.isDefined && (!forTraining || rsentpair.valid)) {
					rawsentpairs += rsentpair
				}
			}

			rawsentpairs
		}

	}

	/* Class SentPairsData:
	*   a data structure of raw data read from files and converted into vector presentation for efficiency
	*/
	class SentPairsData(val data: Array[VectorSentencePair], val sentVocab: Vocab, val wordVocab: Vocab, val featureVocab: Vocab) {

		def nSentPairs: Int = data.size
		def nFeature: Int = this.featureVocab.size

		val nRel: Int = 2  // only 2 different labels for sentence pairs, either paraphrase or not
		val IS_PARAPHRASE = 1
		val IS_NOT_PARAPHRASE = 0
		val relVocab = Array("NonPara", "YesPara")


		override def toString(): String = {
			var output = ""
			for ( i <- 0 until data.length) {
				val datapoint = this.data(i)
				output += datapoint.rel(0) + " | " + datapoint.rel(1) + " | " + datapoint.trendname + " | " + datapoint.origsent + " | " + datapoint.candsent + "\n"
			}
			return output
		}

		def toString(index: Int): String = {
			val datapoint = this.data(index)
			var output = ""

			if (datapoint.rel(IS_NOT_PARAPHRASE) == 1.0) {
				output += this.relVocab(IS_NOT_PARAPHRASE)
			} else if (datapoint.rel(IS_PARAPHRASE) == 1.0) {
				output += this.relVocab(IS_PARAPHRASE)
			}

			output += " | " + datapoint.trendname + " | " + datapoint.origsent + " | " + datapoint.candsent + "\n"
			for (i <- 0 until datapoint.features.length) {
				output += "WordPair #" + i + " : " + this.wordVocab(datapoint.w1ids(i)) + " | " + this.wordVocab(datapoint.w2ids(i)) + " | "

				val strfeatures = Utils.bin2int(datapoint.features(i).toArray).map((f) => this.featureVocab(f))
				output += strfeatures.mkString(" ")
				output += "\n"
			}

			return output
		}
	}

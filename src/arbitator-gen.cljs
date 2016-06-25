(ns arbitrator.gen
  (:require [clojure.test.check.generators :as gen]))


;; Private helpers

(defn- gen-nested-or-val
  [collection-gen val-gen]
  (gen/sized (fn [size]
    (if (zero? size)
      val-gen
      (gen/one-of [
        val-gen
        (collection-gen
          (gen/resize
            (quot size 2)
            (gen-nested-or-val collection-gen val-gen)))])))))

(defn- to-object
  [from-seq]
  (let [obj (js-obj)]
    (doall (map #(aset obj (first %) (second %)) from-seq))
    obj))

(defn- gen-obj
  [key-gen val-gen]
  (gen/fmap to-object (gen/vector (gen/tuple key-gen val-gen))))


;; Generator Builders

(def ^{:export gen.suchThat} genSuchThat gen/such-that)
(def ^{:export gen.notEmpty} genNotEmpty
  (partial gen/such-that (comp not-empty js->clj)))
(def ^{:export gen.map} genMap gen/fmap)
(def ^{:export gen.bind} genBind gen/bind)
(def ^{:export gen.sized} genSized gen/sized)
(def ^{:export gen.resize} genResize gen/resize)
(def ^{:export gen.noShrink} genNoShrink gen/no-shrink)
(def ^{:export gen.shrink} genShrink gen/shrink-2)


;; Simple Generators

(js/goog.exportSymbol "gen.return", gen/return)
(def ^{:export gen.returnOneOf} genReturnOneOf gen/elements)
(def ^{:export gen.oneOf} genOneOf gen/one-of)
(def ^{:export gen.oneOfWeighted} genOneOfWeighted gen/frequency)
(defn ^{:export gen.returnOneOfWeighted} genReturnOneOfWeighted
  [pairs]
  (gen/frequency (map vector
    (map first pairs)
    (map (comp gen/return second) pairs))))
(defn ^{:export gen.nested} genNested
  [collection-gen val-gen]
  (collection-gen (gen-nested-or-val collection-gen val-gen)))


;; Array and Object

(defn ^{:export gen.array} genArray
  ([val-gen min-elements max-elements]
    (gen/fmap to-array (gen/vector val-gen min-elements max-elements)))
  ([val-gen num-elements]
    (gen/fmap to-array (gen/vector val-gen num-elements)))
  ([val-gen-or-arr]
    (gen/fmap to-array
      (if (js/Array.isArray val-gen-or-arr)
        (apply gen/tuple val-gen-or-arr)
        (gen/vector val-gen-or-arr)))))

(defn ^{:export gen.object} genObject
  ([key-gen val-gen]
    (gen/fmap clj->js (gen-obj key-gen val-gen)))
  ([val-gen]
    (gen-obj (gen/resize 16 gen/string-alpha-numeric) val-gen)))

(defn ^{:export gen.arrayOrObject} genArrayOrObject
  [val-gen]
  (gen/one-of [(genArray val-gen) (genObject val-gen)]))


;; JS Primitives

;; TODO: Floating-point Number
;; TODO: UTF8 strings
;; TODO: More performant string generation?

(def ^{:export gen.NaN} genNaN (gen/return js/NaN))
(def ^{:export gen.undefined} genUndefined (gen/return js/undefined))
(def genNull (gen/return nil))
(js/goog.exportSymbol "gen.null", genNull)
(js/goog.exportSymbol "gen.boolean", gen/boolean)

(js/goog.exportSymbol "gen.int", gen/int)
(def ^{:export gen.posInt} genPosInt gen/pos-int)
(def ^{:export gen.negInt} genNegInt gen/neg-int)
(def ^{:export gen.strictPosInt} genStrictPosInt gen/s-pos-int)
(def ^{:export gen.strictNegInt} genStrictNegInt gen/s-neg-int)
(def ^{:export gen.intWithin} genIntWithin gen/choose)

(js/goog.exportSymbol "gen.char", gen/char)
(def ^{:export gen.asciiChar} genAsciiChar gen/char-ascii)
(def ^{:export gen.alphaNumChar} genAlphaNumChar gen/char-alpha-numeric)

(def ^{:export gen.string} genString gen/string)
(def ^{:export gen.asciiString} genAsciiString gen/string-ascii)
(def ^{:export gen.alphaNumString} genAlphaNumString gen/string-alpha-numeric)


;; JSON

(def ^{:export gen.JSONPrimitive} genJSONPrimitive
  (gen/frequency [[1 genNull]
                  [2 gen/boolean]
                  [10 gen/int]
                  [10 gen/string]]))
(def ^{:export gen.JSONValue} genJSONValue
  (gen-nested-or-val genArrayOrObject genJSONPrimitive))
(def ^{:export gen.JSON} genJSON (genObject genJSONValue))


;; JS values, potentially nested

(def ^{:export gen.primitive} genPrimitive
  (gen/frequency [[1 genNaN]
                  [2 genUndefined]
                  [3 genNull]
                  [10 gen/boolean]
                  [50 gen/int]
                  [50 gen/string]]))

(def ^{:export gen.any} genAny
  (gen-nested-or-val genArrayOrObject genPrimitive))

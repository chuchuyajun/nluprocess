(deftemplate th (slot seq) (slot reported) (slot lowc) (slot cleared) (slot oos) (slot cat) (slot T) (slot D) (slot C))(watch all)

;;;;;;;;;;;;;;TD Ontology Rules ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;  

(defrule td_gov_owl
	(_td ? ?gov ?)
	(not (_fact ?gov rdf:type owl:NamedIndividual))
=>
	(assert (_fact ?gov rdf:type owl:NamedIndividual))
)

(defrule td_dep_owl
	(_td ? ? ?dep)
	(not (_fact ?dep rdf:type owl:NamedIndividual))
=>
	(assert (_fact ?dep rdf:type owl:NamedIndividual))
)  

(defrule td_gov_lemma
	(_td ? ?gov ?)
	(_lema ?gov ?lema)
	(not (_fact ?gov lemma:value ?))
=>
	(assert (_fact ?gov lemma:value (str-merge lemma: ?lema)))
)

(defrule td_dep_lemma
	(_td ? ? ?dep)
	(_lema ?dep ?lema)
	(not (_fact ?dep lemma:value ?))
=>
	(assert (_fact ?dep lemma:value (str-merge lemma: ?lema)))
)

(defrule pos_wh
	(_pos ?wh WRB|WP|WDT)
	(_lema ?wh ?lema)
	(not (_fact ?wh var:type ?lema))
=>
	(assert (_fact ?wh var:type ?lema))
)  
	
(defrule td_ner_property
	(_ner ?word ?attr)
	(not (_fact ?attr rdf:type owl:Class))
=>
	(assert (_fact ?attr rdf:type owl:Class))
	(assert (_fact (str-merge has ?attr) rdf:type owl:ObjectProperty))
)

(defrule td_default_data_property
	(initial-fact)
=>
	(assert (_fact comparedDegree rdf:type owl:DatatypeProperty))
	(assert (_fact has-VALUE rdf:type owl:DatatypeProperty))
	(assert (_fact isMODAL rdf:type owl:ObjectProperty))
)

(defrule td_ner_attr
	(_fact ?attr rdf:type owl:Class)
	(_ner ?word ?attr)
=>
	(assert (_fact ?word rdf:type ?attr))
)

(defrule td_adj_adv_no_ner
	(_pos ?adj JJ|JJR|JJS|RB|RBR|RBS)
	(_lema ?adj ?lema)
	(not (_ner ?adj ?))
=>
	(assert (_fact ?lema rdf:type owl:Class))
	(assert (_fact (str-merge has ?lema) rdf:type owl:ObjectProperty))
	(assert (_fact ?adj rdf:type ?lema))
)

(defrule number-individule
	(_fact ?token hasNUMBER ?number)
=>
	(assert (_fact (str-merge "number-" (getTokenIndex ?token)) rdf:type owl:NamedIndividual))
)

(defrule age-individule
	(_fact ?token hasAGE ?a) ; John-1x3 :hasAGE :age ; :age :has-VALUE "30" 
	(not (_fact ?age&:(regexp ?age "^age-") rdf:type owl:NamedIndividual))
=>
	(assert (_fact (str-merge "age-" (getTokenIndex ?token)) rdf:type owl:NamedIndividual))
)

;;;;;;;;;;;;;;Domain Ontology Rules ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;John/NN is 30/CD-NUMBER	
(defrule nsubj-cop-ner-number
	(_pos ?adj CD)	
	(_td nsubj ?adj ?subj)
	(_td cop ?adj ?)
	(_ner ?adj ?attr)
	(_lema ?adj ?lema)
=>
	(assert (_fact ?subj (str-merge has ?attr) (str-merge "number-"  (getTokenIndex ?subj))))
	(assert (_fact (str-merge "number-" (getTokenIndex ?subj)) has-VALUE (str-cat ?lema)))
)

;Apple is red/JJ- NER/LEMA
; to avoid "John has a card that is valid." (_td nsubj valid-1x10 which-1x8) (_td cop valid-1x10 are-1x9)
(defrule nsubj-cop-ner-adj_jj
	(_pos ?adj JJ|RB)	
	(_td nsubj ?adj ?subj)
	;(_td cop ?adj ?)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(not (_td rcmod ? ?adj))
	(not (_td advmod ?adj ?adv&:(regexp ?adv "^more-|^most-")))  ; "more + important" will be combined.
	(not (_td xcomp ?adj ?)) ; John is ready to leave - leave will be governer term not ready
=>
	(assert (_fact ?adj comparedDegree 0))
	(assert (_fact ?subj (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

; John is more important.
(defrule nsubj-cop-ner-adj_jjr_comp
	(_pos ?adj JJ|RB)	
	(_td nsubj ?adj ?subj)
	;(_td cop ?adj ?)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(not (_td rcmod ? ?adj))
	(_td advmod ?adj ?adv&:(regexp ?adv "^more-"))  ; "more + important" will be combined.
=>
	(assert (_fact ?adj comparedDegree 1))
	(assert (_fact ?subj (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

; John is the most important.
(defrule nsubj-cop-ner-adj_jjs_comp
	(_pos ?adj JJ|RB)	
	(_td nsubj ?adj ?subj)
	;(_td cop ?adj ?)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(not (_td rcmod ? ?adj))
	(_td advmod ?adj ?adv&:(regexp ?adv "^most-"))  ; "more + important" will be combined.
=>
	(assert (_fact ?adj comparedDegree 2))
	(assert (_fact ?subj (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;Apple is redder/JJR- NER/LEMA
(defrule nsubj-cop-ner-adj_jjr
	(_pos ?adj JJR|RBR)	
	(_td nsubj ?adj ?subj)
	;(_td cop ?adj ?)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(not (_td rcmod ? ?adj))
=>
	(assert (_fact ?adj comparedDegree 1))
	(assert (_fact ?subj (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;Apple is redest/JJS- NER/LEMA
(defrule nsubj-cop-ner-adj_jjs
	(_pos ?adj JJS|RBS)	
	(_td nsubj ?adj ?subj)
	;(_td cop ?adj ?)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(not (_td rcmod ? ?adj))
=>
	(assert (_fact ?adj comparedDegree 2))
	(assert (_fact ?subj (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;8/eight/CD apples
(defrule num-nn
	(_td num ?noun ?num)
	(_ner ?num ?attr)
	(not (_td prep_of ?noun ?))
	(_lema ?num ?lema)
=>
	;(assert (_fact ?noun (str-merge has ?attr) (str-cat ?lema)))
	(assert (_fact ?noun (str-merge has ?attr) (str-merge "number-"  (getTokenIndex ?noun))))
	(assert (_fact (str-merge "number-" (getTokenIndex ?noun)) has-VALUE (str-cat ?lema)))
)

;8 kg of apples
(defrule num-nn-unit
	(_td num ?noun ?num)
	(_ner ?num ?attr)
	(_td prep_of ?noun ?item)
	(_lema ?num ?lema)
	(test (regexp ?attr "^LONGNESS|^WEIGHT|^AREA|^MONEY"))  ; TODO - need to add more NER
=>
	(assert (_fact ?item (str-merge has ?attr) (str-cat ?noun)))
	(assert (_fact ?noun has-VALUE ?lema))
)

; should/could/can do something
(defrule aux-vb
	(_td aux ?vb ?modal)
	(_pos ?modal MD)
=>
	(assert (_fact ?vb isMODAL ?modal))
)

;good/JJ teacher/NN
(defrule amod-jj-nn
	(_td amod ?noun ?adj)
	(_pos ?noun NNP|NNS|NN)
	(_pos ?adj JJ|RB)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(not (_td advmod ?adj ?adv&:(regexp ?adv "^more-|^most-")))  ; "more + important" will be combined.
=>
	(assert (_fact ?adj comparedDegree 0))
	(assert (_fact ?noun (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;more important/JJ teacher/NN
(defrule amod-jjr-nn_comp
	(_td amod ?noun ?adj)
	(_pos ?noun NNP|NNS|NN)
	(_pos ?adj JJ|RB)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(_td advmod ?adj ?adv&:(regexp ?adv "^more-"))  ; "more + important" will be combined.
=>
	(assert (_fact ?adj comparedDegree 1))
	(assert (_fact ?noun (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;most important/JJ teacher/NN
(defrule amod-jjs-nn_comp
	(_td amod ?noun ?adj)
	(_pos ?noun NNP|NNS|NN)
	(_pos ?adj JJ|RB)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(_td advmod ?adj ?adv&:(regexp ?adv "^most-"))  ; "more + important" will be combined.
=>
	(assert (_fact ?adj comparedDegree 2))
	(assert (_fact ?noun (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;better/JJ teacher/NN
(defrule amod-jjr-nn
	(_td amod ?noun ?adj)
	(_pos ?noun NNP|NNS|NN)
	(_pos ?adj JJR|RBR)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
=>
	(assert (_fact ?adj comparedDegree 1))
	(assert (_fact ?noun (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;best/JJ teacher/NN
(defrule amod-jjs-nn
	(_td amod ?noun ?adj)
	(_pos ?noun NNP|NNS|NN)
	(_pos ?adj JJS|RBS)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
=>
	(assert (_fact ?adj comparedDegree 2))
	(assert (_fact ?noun (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;the Hilton Marietta Conference Center.
(defrule nn-jj-no-ner
	(_td nn ?ng ?nd)
	(_lema ?nd ?lema)
	(not (_ner ?nd ?))
=>
	(assert (_fact ?ng (str-merge has ?lema) ?nd))
	(assert (_fact ?nd has-VALUE (str-cat ?lema)))
)

;the Hilton Marietta Conference Center.
(defrule nn-jj-ner
	(_td nn ?ng ?nd)
	(_lema ?nd ?lema)
	(_fact ?nd rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
=>
	(assert (_fact ?ng (str-merge has ?attr) ?nd))
	(assert (_fact ?nd has-VALUE (str-cat ?lema)))
)

;John/NN is a teacher/NN 
; to avoid "John has a card that is valid." (_td nsubj valid-1x10 which-1x8) (_td cop valid-1x10 are-1x9)
(defrule nsubj-cop-nn_no_ner
	(_td nsubj ?noun ?subj)
	(_lema ?noun ?nl)
	(_pos ?noun NNP|NNS|NN|NNPS)	
	(_td cop ?noun ?)
	(not (_ner ?noun ?))
=>
	(assert (_fact ?subj rdf:type ?nl))
	(assert (_fact ?nl rdf:type owl:Class))
)

;The girl/NN is Mary/NN - ner:PERSON
(defrule nsubj-cop-nn_ner
	(_td nsubj ?noun ?subj)
	(_pos ?noun NNP|NNS|NN|NNPS)	
	(_td cop ?noun ?)
	(_ner ?noun ?attr)
	(_lema ?noun ?nl)
=>
	(assert (_fact ?subj rdf:type ?nl))
	(assert (_fact ?nl rdf:type owl:Class))
	(assert (_fact ?subj rdf:type ?attr))
)

;John runs
(defrule nsubj
	(_td nsubj ?vb ?subj)
	(_pos ?vb VB|VBD|VBG|VBN|VBP|VBZ)
	(not (_td dobj ?vb ?))
	(not (_td rcmod ? ?vb))
=>
	(assert (_fact ?subj nsubj ?vb))
)

;John owns a computer
(defrule nsubj-dobj
	(_td nsubj ?vb ?subj)
	(_pos ?vb VB|VBD|VBG|VBN|VBP|VBZ)
	(_td dobj ?vb ?dobj)
	(not (_td iobj ?vb ?))
	(not (_td rcmod ? ?vb))
=>
	(assert (_fact ?subj nsubj ?vb))
	(assert (_fact ?vb dobj ?dobj))
)

;John gives Mary a computer
(defrule nsubj-dobj-iobj
	(_td nsubj ?vb ?subj)
	(_pos ?vb VB|VBD|VBG|VBN|VBP|VBZ)
	(_td dobj ?vb ?dobj)
	(_td iobj ?vb ?iobj)
	(not (_td rcmod ? ?vb))
=>
	(assert (_fact ?subj nsubj ?vb))
	(assert (_fact ?vb dobj ?dobj))
	(assert (_fact ?vb iobj ?iobj))
)

;passive - John is hit.
(defrule nsubj-no-dobj-passive
	(_td nsubjpass ?vb ?dobj)
	(_td auxpass ?vb ?be)
	(not (_td agent ?vb ?))
	(_pos ?vb VBN)
=>
	(assert (_fact ?vb dobj ?dobj))
)

;pssive - John is hit by Tom.
(defrule nsubj-dobj-passive
	(_td nsubjpass ?vb ?dobj)
	(_td auxpass ?vb ?be)
	(_td agent ?vb ?subj)
	(_pos ?vb VBN)
=>
	(assert (_fact ?subj nsubj ?vb))
	(assert (_fact ?vb dobj ?dobj))
)

;pssive - John is given a computer by Tom.
(defrule nsubj-dobj-iobj-passive
	(_td nsubjpass ?vb ?dobj)
	(_td auxpass ?vb ?be)
	(_td agent ?vb ?subj)
	(_td dobj ?vb ?iobj)
	(_pos ?vb VBN)
=>
	(assert (_fact ?subj nsubj ?vb))
	(assert (_fact ?vb dobj ?dobj))
	(assert (_fact ?vb iobj ?iobj))
)

;finish the job very quickly - modifier_adv
(defrule vb-rb-advmod_hasattr
	(_td advmod ?indv ?adv)
	(_pos ?adv RB)
	(_lema ?adv ?lema)
	(_fact ?adv rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
=>
	(assert (_fact ?indv (str-merge has ?attr) ?adv))
	(assert (_fact ?adv has-VALUE (str-cat ?lema)))
	;(assert (_fact ?adv comparedDegree 0))
)

;run quickly   - only when no more|most exist generate degree, otherwise it will be generate in other rules.
(defrule vb-rb-advmod_degree
	(_td advmod ?indv ?adv)
	(_pos ?adv RB)
	(_lema ?adv ?lema)
	(not (_td advmod ?adv ?comp&:(regexp ?comp "^most-|^more-")))
=>
	(assert (_fact ?adv comparedDegree 0))
)

; quicker
(defrule vb-rbr-advmod
	(_td advmod ?indv ?adv)
	(_pos ?adv RBR)
	(_lema ?adv ?lema)
	(_fact ?adv rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(test (not (regexp ?adv "^more-")))
=>
	(assert (_fact ?indv (str-merge has ?attr) ?adv))
	(assert (_fact ?adv has-VALUE (str-cat ?lema)))
	(assert (_fact ?adv comparedDegree 1))
)
; quickest
(defrule vb-rbs-advmod
	(_td advmod ?indv ?adv)
	(_pos ?adv RBS)
	(_lema ?adv ?lema)
	(_fact ?adv rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(test (not (regexp ?adv "^most-")))
=>
	(assert (_fact ?indv (str-merge has ?attr) ?adv))
	(assert (_fact ?adv has-VALUE (str-cat ?lema)))
	(assert (_fact ?adv comparedDegree 2))
)



; more quickly
(defrule vb-rbr-advmod_comp
	(_td advmod ?indv ?adv)
	(_pos ?adv RBR)
	(_pos ?indv RB)
	(_lema ?adv ?lema)
	(test (regexp ?adv "^more-"))
=>
	(assert (_fact ?indv comparedDegree 1))
)

; most quickly
(defrule vb-rbs-advmod_comp
	(_td advmod ?indv ?adv)
	(_pos ?adv RBR)
	(_pos ?indv RB)
	(_lema ?adv ?lema)
	(test (regexp ?adv "^most-"))
=>
	(assert (_fact ?indv comparedDegree 2))
)

;John's dog is black. - Poss relation
(defrule poss-relation
	(_td poss ?n ?nsubj)
=>
	(assert (_fact ?nsubj owns ?n))
)

;the elements of table is black. - OF poss relation
(defrule poss-of-relation
	(_td prep_of ?n ?nsubj)
	(_pos ?n NNP|NNS|NN|NNPS)
	(_pos ?nsubj NNP|NNS|NN|NNPS)
=>
	(assert (_fact ?nsubj owns ?n))
)

; John is in the house. prep_*
(defrule prep_simple
	(_td ?prep ?vb ?dest)
	(test (regexp ?prep "^prep_"))
	(test (not (regexp ?prep "^prep_of")))
	(not (_ner ?dest ?attr&:(regexp ?attr "^DATE|^LOCATION")))
=>
	(assert (_fact ?vb ?prep ?dest))
)

; John lives in Chicago. prep_*
(defrule prep_simple_attr_location
	(_td ?prep ?vb ?dest)
	(test (regexp ?prep "^prep_"))
	(test (not (regexp ?prep "^prep_of")))
	(_ner ?dest ?attr)
	(_lema ?dest ?lema)
	(test (regexp ?attr "^LOCATION")) 
=>
	(assert (_fact ?vb (str-merge has ?attr) ?dest))
	(assert (_fact ?dest has-VALUE (str-cat ?lema)))
)

; Olympic starts at July. prep_*
(defrule prep_simple_attr_nsubj_date
	(_td ?prep ?vb ?dest)
	(test (regexp ?prep "^prep_"))
	(test (not (regexp ?prep "^prep_of")))
	(not (_td dobj ?vb ?))
	(_td nsubj ?vb ?subj)
	(_ner ?dest ?attr)
	(_lema ?dest ?lema)
	(test (regexp ?attr "^DATE")) 
=>
	(assert (_fact ?subj (str-merge has ?attr) ?dest))
	(assert (_fact ?dest has-VALUE (str-cat ?lema)))
)

;Jason will update the DD document in July.
(defrule prep_simple_attr_nsubj_vb_obj_date
	(_td ?prep ?vb ?dest)
	(test (regexp ?prep "^prep_"))
	(test (not (regexp ?prep "^prep_of")))
	(_td dobj ?vb ?obj)
	(_td nsubj ?vb ?subj)
	(_ner ?dest ?attr)
	(_lema ?dest ?lema)
	(test (regexp ?attr "^DATE")) 
=>
	(assert (_fact ?obj (str-merge has ?attr) ?dest))
	(assert (_fact ?dest has-VALUE (str-cat ?lema)))
)


; Robert have a meeting at Tuesday. prep_*
(defrule prep_simple_attr_nsubj_nn_obj_date
	(_td ?prep ?obj ?dest)
	(test (regexp ?prep "^prep_"))
	(test (not (regexp ?prep "^prep_of")))
	(_td dobj ?vb ?obj)
	(_td nsubj ?vb ?subj)
	(_ner ?dest ?attr)
	(_lema ?dest ?lema)
	(test (regexp ?attr "^DATE")) 
=>
	(assert (_fact ?obj (str-merge has ?attr) ?dest))
	(assert (_fact ?dest has-VALUE (str-cat ?lema)))
)

;; John will start to work at July.
(defrule prep_simple_attr_xcomp_date
	(_td ?prep ?vbwh ?dest)
	(test (regexp ?prep "^prep_"))
	(test (not (regexp ?prep "^prep_of")))
	(_td xcomp ?vb ?vbwh)
	(not (_td dobj ?vb ?obj))
	(_td nsubj ?vb ?subj)
	(_ner ?dest ?attr)
	(_lema ?dest ?lema)
	(test (regexp ?attr "^DATE")) 
=>
	(assert (_fact ?vbwh (str-merge has ?attr) ?dest))
	(assert (_fact ?dest has-VALUE (str-cat ?lema)))
)

;; Lucy will plan to go vacation at July.
(defrule prep_simple_attr_xcomp_obj_date
	(_td ?prep ?vbwh ?dest)
	(test (regexp ?prep "^prep_"))
	(test (not (regexp ?prep "^prep_of")))
	(_td xcomp ?vb ?vbwh)
	(_td dobj ?vbwh ?obj)
	(_td nsubj ?vb ?subj)
	(_ner ?dest ?attr)
	(_lema ?dest ?lema)
	(test (regexp ?attr "^DATE")) 
=>
	(assert (_fact ?obj (str-merge has ?attr) ?dest))
	(assert (_fact ?dest has-VALUE (str-cat ?lema)))
)

; John will go vacation today.
(defrule tmod_date
	(_td tmod ?vb ?date)
	(_td dobj ?vb ?obj)
	(_ner ?date DATE)
	(_lema ?date ?lema)
=>
	(assert (_fact ?obj hasDATE ?date))
	(assert (_fact ?date has-VALUE (str-cat ?lema)))
)

; John will start to work today.
(defrule tmod_date_xcomp
	(_td tmod ?vb ?date)
	(_td xcomp ?vbwh ?vb)
	(not (_td dobj ?vb ?obj))
	(_ner ?date DATE)
	(_lema ?date ?lema)
=>
	(assert (_fact ?vb hasDATE ?date))
	(assert (_fact ?date has-VALUE (str-cat ?lema)))
)	


;;;;;;;;Complex sentense;;;;;;;;;;;;;;

;A customer has a card that are valid.
(defrule rcmod_jj
	(_td rcmod ?noun ?adj)
	(_pos ?adj JJ)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
=>
	(assert (_fact ?adj comparedDegree 0))
	(assert (_fact ?noun (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

(defrule rcmod_jjr
	(_td rcmod ?noun ?adj)
	(_pos ?adj JJR)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
=>
	(assert (_fact ?adj comparedDegree 1))
	(assert (_fact ?noun (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

(defrule rcmod_jjs
	(_td rcmod ?noun ?adj)
	(_pos ?adj JJS)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
=>
	(assert (_fact ?adj comparedDegree 2))
	(assert (_fact ?noun (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;John has a computer which runs fast.
(defrule rcmod_vb
	(_td rcmod ?noun ?vb)
	(_pos ?vb VB|VBD|VBG|VBN|VBP|VBZ)
	(not (_td dobj ?vb ?))
=>
	(assert (_fact ?noun nsubj ?vb))
)

;John likes to swim.
(defrule xcomp_vb
	(_td xcomp ?vb ?xvb)
	(_pos ?vb VB|VBD|VBG|VBN|VBP|VBZ)
	(not (_dobj ?xvb ?dobj))
=>
	(assert (_fact ?vb nsubj ?xvb))
)

;John allows Mary to touch the new computer.
(defrule xcomp_vb_dobj
	(_td xcomp ?vb ?xvb)
	(_pos ?vb VB|VBD|VBG|VBN|VBP|VBZ)
	(_td dobj ?xvb ?dobj)
=>
	(assert (_fact ?vb nsubj ?xvb))
	(assert (_fact ?xvb dobj ?dobj))
)	
	
;John is ready to leave.
(defrule xcomp_jj_vb
	(_td xcomp ?adj ?vb)
	(_td nsubj ?adj ?sub)
	(_pos ?adj JJ|RB)
	(_lema ?adj ?lema)
	(_fact ?adj rdf:type ?attr)
	(_fact ?attr rdf:type owl:Class)
	(_pos ?vb VB|VBD|VBG|VBN|VBP|VBZ)
=>
	(assert (_fact ?sub nsubj ?vb))
	(assert (_fact ?vb (str-merge has ?attr) ?adj))
	(assert (_fact ?adj has-VALUE (str-cat ?lema)))
)

;;;;;;;;;;;;;;;;;;;;;;Age Expression;;;;;;;;;;;;
;Jason is 47 years old.
(defrule age_npadvmod
	(_td npadvmod ?old ?yr)
	(_td num ?yr ?num)
	(_td nsubj ?old ?subj)
	(test (regexp ?old "^old-"))
	(_pos ?num CD)
	(_lema ?num ?lema)
=>
	(assert (_fact ?subj hasAGE (str-merge "age-"  (getTokenIndex ?subj))))
	(assert (_fact (str-merge "age-" (getTokenIndex ?subj)) has-VALUE (str-cat ?lema)))
	
)

;John is age of 5.
(defrule age_of
	(_td nsubj ?age ?subj)
	(_td prep_of ?age ?num)
	(test (regexp ?age "^age-"))
	(_pos ?num CD)
	(_lema ?num ?lema)
=>
	(assert (_fact ?subj hasAGE ?age))
	(assert (_fact ?age has-VALUE (str-cat ?lema)))
	
)	

;John is age of 5 years old.
(defrule age_of_yrs
	(_td npadvmod ?old ?yr)
	(_td num ?yr ?num)
	(_td nsubj ?age ?subj)
	(_td prep_of ?age ?old)
	(test (regexp ?old "^old-"))
	(test (regexp ?age "^age-"))
	(_lema ?num ?lema)
=>
	(assert (_fact ?subj hasAGE (str-merge "age-"  (getTokenIndex ?subj))))
	(assert (_fact (str-merge "age-" (getTokenIndex ?subj)) has-VALUE (str-cat ?lema)))
)	

;John's age is 5.
(defrule age_poss
	(_td poss ?age ?subj)
	(_td nsubj ?num ?age)
	(test (regexp ?age "^age-"))
	(_pos ?num CD)
	(_lema ?num ?lema)
=>
	(assert (_fact ?subj hasAGE (str-merge "age-"  (getTokenIndex ?subj))))
	(assert (_fact (str-merge "age-" (getTokenIndex ?subj)) has-VALUE (str-cat ?lema)))
)	

;The age of Tom is 5.
(defrule age_poss_of
	(_td prep_of ?age ?subj)
	(_td nsubj ?num ?age)
	(test (regexp ?age "^age-"))
	(_pos ?num CD)
	(_pos ?subj NNP|NNS|NN|NNPS)
	(_lema ?num ?lema)
=>
	(assert (_fact ?subj hasAGE (str-merge "age-"  (getTokenIndex ?subj))))
	(assert (_fact (str-merge "age-" (getTokenIndex ?subj)) has-VALUE (str-cat ?lema)))
)	
	
;How old is John?
(defrule q_age_how_old
	(_td advmod ?old ?how)
	(_td attr ?be ?old)
	(_td nsubj ?be ?subj)
	(_pos ?how WRB)
	(test (regexp ?old "^old-"))
=>
	(assert (_fact ?subj hasAGE ?how))
	(assert (_fact ?how has-VALUE (str-cat var: ?how)))
)	

;what is John's age?
(defrule q_age_what_poss
	(_td attr ?be ?wh)
	(_td poss ?age ?subj)
	(_td nsubj ?be ?age)
	(test (regexp ?age "^age-"))
	(_pos ?wh WP)
=>
	(assert (_fact ?subj hasAGE ?wh))
	(assert (_fact ?wh has-VALUE (str-cat var: ?wh)))
)	


;what is the age of John?
(defrule q_age_what_poss_of
	(_td attr ?be ?wh)
	(_td nsubj ?be ?age)
	(_td prep_of ?age ?subj)
	(test (regexp ?age "^age-"))
	(_pos ?wh WP)
=>
	(assert (_fact ?subj hasAGE ?wh))
	(assert (_fact ?wh has-VALUE (str-cat var: ?wh)))
)


;;;;;;;;;;;;;;;;;;;;;;Questions;;;;;;;;;;;;;;;;;;
; what is a phone?
(defrule q_attr_cop
	(_td attr ?be ?wh)
	(_pos ?wh WP)
	(_td nsubj ?be ?subj)
	(_lema ?wh ?lema)
	(test (not (regexp ?wh "^who-")))
	(test (not (regexp ?subj "^age-")))   ;AGE term Exception
=> 
	(assert (_fact ?subj rdf:type ?wh))
)

; who is a teacher?
(defrule q_attr_cop_who
	(_td attr ?be ?wh)
	(_pos ?wh WP)
	(_td nsubj ?be ?subj)
	(_lema ?wh ?lema)
	(test (regexp ?wh "^who-"))
=>
	(assert (_fact ?wh rdf:type ?subj))
)

; where does John work?
; where is John?
(defrule q_advmod_vb_where
	(_td advmod ?vb ?wh)
	(_pos ?wh WRB)
	(_td nsubj ?vb ?subj)
	(test (regexp ?wh "^where-"))
=>
	(assert (_fact ?vb hasLOCATION ?wh))
	(assert (_fact ?wh has-VALUE (str-cat var: ?wh)))
)
; when will Olympic start?
(defrule q_advmod_vb_when_subj
	(_td advmod ?vb ?wh)
	(_td nsubj ?vb ?subj)
	(_pos ?wh WRB)
	(not (_td xcomp ?vb ?))
	(not (_td dobj ?vb ?))
	(test (regexp ?wh "^when-"))
=>
	(assert (_fact ?subj hasDATE ?wh))
	(assert (_fact ?wh has-VALUE (str-cat var: ?wh)))
)

; when will Robert have a meeting?
(defrule q_advmod_vb_when_subj_obj
	(_td advmod ?vb ?wh)
	(_td nsubj ?vb ?subj)
	(_pos ?wh WRB)
	(not (_td xcomp ?vb ?))
	(_td dobj ?vb ?dobj)
	(test (regexp ?wh "^when-"))
=>
	(assert (_fact ?dobj hasDATE ?wh))
	(assert (_fact ?wh has-VALUE (str-cat var: ?wh)))
)

; when will John start to work?
(defrule q_advmod_vb_when_subj_xcomp
	(_td advmod ?vb ?wh)
	(_pos ?wh WRB)
	(_td nsubj ?vb ?subj)
	(_td xcomp ?vb ?vbwh)
	(not (_td dobj ?vbwh ?dobj))
	(test (regexp ?wh "^when-"))
=>
	(assert (_fact ?vbwh hasDATE ?wh))
	(assert (_fact ?wh has-VALUE (str-cat var: ?wh)))
)

; when will Lucy plan to go vacation?
(defrule q_advmod_vb_when_subj_dobj_xcomp
	(_td advmod ?vb ?wh)
	(_pos ?wh WRB)
	(_td nsubj ?vb ?subj)
	(_td xcomp ?vb ?vbwh)
	(_td dobj ?vbwh ?dobj)
	(test (regexp ?wh "^when-"))
=>
	(assert (_fact ?dobj hasDATE ?wh))
	(assert (_fact ?wh has-VALUE (str-cat var: ?wh)))
)



  
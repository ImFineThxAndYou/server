from flask import Flask, request, jsonify
import spacy
import re
from konlpy.tag import Okt

app = Flask(__name__)

# Load English spaCy model
nlp_en = spacy.load("en_core_web_sm")

# Load Korean NLP
okt = Okt()

# 너의 사전에 존재하는 pos 리스트 (정확한 매핑 기준)
dictionary_pos_set = {
    "indefinite article", "definite article", "verb", "noun", "adjective", "adverb",
    "preposition", "conjunction", "exclamation", "determiner", "pronoun",
    "auxiliary verb", "modal verb", "ordinal number", "number", "cream noun",
    "one pronoun", "cent adjective", "cent adverb", "cent noun", "linking verb",
    "infinitive marker"
}

# 영어 tag_ → 너의 사전 pos 값으로 변환
tag_to_dict_pos = {
    "DT": "indefinite article",   # a, an (the는 아래에서 따로 처리)
    "VB": "verb",
    "VBD": "verb",
    "VBG": "verb",
    "VBN": "verb",
    "VBP": "verb",
    "VBZ": "verb",
    "MD": "modal verb",
    "AUX": "auxiliary verb",
    "NN": "noun",
    "NNS": "noun",
    "NNP": "noun",  # proper noun 처리 원하면 변경 가능
    "NNPS": "noun",
    "PRP": "pronoun",
    "PRP$": "pronoun",
    "WP": "pronoun",
    "WP$": "pronoun",
    "JJ": "adjective",
    "JJR": "adjective",
    "JJS": "adjective",
    "RB": "adverb",
    "RBR": "adverb",
    "RBS": "adverb",
    "IN": "preposition",  # 전치사 or 종속 접속사
    "CC": "conjunction",
    "UH": "exclamation",
    "PDT": "determiner",
    "WDT": "determiner",
    "CD": "number",
    "TO": "infinitive marker"
}

# 품사 한국어 fullname
allowed_pos_fullnames = {
    "감탄사", "고유 명사", "관형사", "대명사", "동사",
    "명사", "보조 용언", "부사", "분석 불능", "수사", "의존 명사", "형용사"
}

# 품사 한국어 변환
pos_to_korean = {
    "Exclamation": "감탄사",
    "ProperNoun": "고유 명사",
    "Determiner": "관형사",
    "Pronoun": "대명사",
    "Verb": "동사",
    "Noun": "명사",
    "AuxVerb": "보조 용언",
    "Adverb": "부사",
    "Alpha": "분석 불능",
    "Number": "수사",
    "DependentNoun": "의존 명사",
    "Adjective": "형용사"
}

# 문장에서 영어 추출
def extract_english(text):
    return " ".join(re.findall(r"[a-zA-Z]+", text))

#문장에서 한글 추출
def extract_korean(text):
    return " ".join(re.findall(r"[가-힣]+", text))

# 혼합 문장(순수 문장 ok) 분석
def analyze_mixed_text(text):
    result = []

    # ✅ 영어 처리
    en_text = extract_english(text)
    if en_text.strip():
        doc = nlp_en(en_text)
        for token in doc:
            if not token.is_stop and token.is_alpha:
                tag = token.tag_
                word_text = token.text.lower()
                mapped_pos = None

                # 특수 처리: "the"는 definite article로
                if word_text == "the":
                    mapped_pos = "definite article"
                else:
                    mapped_pos = tag_to_dict_pos.get(tag)

                if mapped_pos and mapped_pos in dictionary_pos_set:
                    result.append({
                        "word": token.text,
                        "pos": mapped_pos,
                        "lang": "en"
                    })

    # ✅ 한국어 처리 (KoNLPy)
    ko_text = extract_korean(text)
    if ko_text.strip():
        for word, pos in okt.pos(ko_text, stem=True):
            kor_pos = pos_to_korean.get(pos)
            if kor_pos in allowed_pos_fullnames:
                result.append({
                    "word": word,
                    "pos": kor_pos,
                    "lang": "ko"
                })

    return result

#단일 문장 분석
@app.route("/analyze/mixed", methods=["POST"])
def analyze_mixed():
    data = request.get_json()
    text = data.get("text", "")
    result = analyze_mixed_text(text)
    return jsonify(result)

# 배치 문장 분석
@app.route("/analyze/mixed-batch", methods=["POST"])
def analyze_mixed_batch():
    data = request.get_json()
    messages = data.get("messages", [])

    all_tokens = []
    for msg in messages:
        text = msg.get("content", "")
        message_id = msg.get("messageId") or msg.get("id")

        analyzed_tokens = analyze_mixed_text(text)

        #각 토큰에 messageId와 example 추가
        for token in analyzed_tokens:
            token["sourceMessageId"] = message_id
            token["example"] = text

        all_tokens.extend(analyzed_tokens)

    return jsonify(all_tokens)




@app.route("/health")
def health():
    return "ok", 200

# 앱 실행
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)

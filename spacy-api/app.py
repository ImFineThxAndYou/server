from flask import Flask, request, jsonify
import spacy
import re
from konlpy.tag import Okt

app = Flask(__name__)

# Load English spaCy model
nlp_en = spacy.load("en_core_web_sm")

# Load Korean NLP
okt = Okt()

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

    # 영어 처리
    en_text = extract_english(text)
    if en_text.strip():
        doc = nlp_en(en_text)
        for token in doc:
            if not token.is_stop and token.is_alpha:
                result.append({
                    "text": token.text,
                    "pos": token.pos_,
                    "lang": "en"
                })

    # ✅ 한국어 처리 (KoNLPy)
    ko_text = extract_korean(text)
    if ko_text.strip():
        for word, pos in okt.pos(ko_text, stem=True):
            kor_pos = pos_to_korean.get(pos)
            if kor_pos in allowed_pos_fullnames:
                result.append({
                    "text": word,
                    "pos": kor_pos,
                    "lang": "ko"
                })

    return result


@app.route("/analyze/mixed", methods=["POST"])
def analyze_mixed():
    data = request.get_json()
    text = data.get("text", "")
    result = analyze_mixed_text(text)
    return jsonify(result)


@app.route("/health")
def health():
    return "ok", 200

# 앱 실행
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)

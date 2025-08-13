# nlp_service/main.py
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Dict
from sentence_transformers import SentenceTransformer, util

app = FastAPI(title="NLP Tagging Service")

@app.get("/health")
def health():
    return {"ok": True}

# ===== 1. 태그 + 설명 =====
tag_descriptions = {
    "문화 & 예술": "Culture & Arts: traditional culture, performance, art, painting, sculpture, museum, literature, poetry, novel, exhibition, theater, drama, festival",
    "음식 & 요리": "Food & Cooking: cooking, recipe, chef, cuisine, meal, dish, ingredient, gastronomy, dining, restaurant, kitchen, baking",
    "디저트 & 음료": "Desserts & Beverages: coffee, tea, latte, espresso, cappuccino, cake, bread, dessert, pastry, cookie, smoothie, juice, milkshake",
    "여행 & 관광": "Travel & Tourism: travel, trip, tour, tourism, destination, vacation, holiday, beach, island, mountain, hiking, sightseeing",
    "기술 & IT": "Technology & IT: technology, computer, software, hardware, programming, coding, artificial intelligence, AI, robotics, gadget, innovation",
    "모바일 & 앱": "Mobile & Apps: smartphone, mobile, cell phone, android, ios, application, app, ux, ui, mobile software",
    "스포츠": "Sports: sport, soccer, football, basketball, baseball, tennis, golf, fitness, gym, yoga, swimming, running",
    "건강 & 웰빙": "Health & Wellness: health, fitness, diet, nutrition, meditation, mental health, wellness, exercise, workout, healthy lifestyle",
    "패션 & 뷰티": "Fashion & Beauty: fashion, clothing, style, outfit, dress, makeup, cosmetics, skincare, beauty products, hairstyle, modeling",
    "경제 & 금융": "Economy & Finance: economy, economic, finance, financial, investment, stock, trading, market, business, real estate, banking",
    "교육 & 학습": "Education & Learning: education, study, studying, school, learning, course, online course, lecture, class, training, self-development",
    "엔터테인먼트": "Entertainment: entertainment, movie, film, cinema, drama, anime, animation, concert, musical, show, performance",
    "게임": "Games: game, gaming, console, pc game, mobile game, playstation, xbox, nintendo, esports, online game",
    "사회 & 정치": "Society & Politics: society, politics, political, government, law, policy, election, social issues, democracy, activism",
    "과학": "Science: science, scientific, physics, chemistry, biology, astronomy, geology, experiment, research",
    "환경 & 자연": "Environment & Nature: environment, environmental, nature, climate, climate change, eco-friendly, wildlife, recycling, conservation",
    "음악": "Music: music, song, track, album, band, instrument, guitar, piano, concert, festival, performance",
    "언어 & 문화교류": "Languages & Cultural Exchange: language, foreign language, translation, bilingual, multilingual, language exchange, culture exchange",
    "역사": "History: history, historical, ancient history, modern history, heritage, war, civilization, historical event",
    "자동차 & 모빌리티": "Automobiles & Mobility: automobile, car, vehicle, bike, motorcycle, transportation, driving, road trip",
    "인테리어 & 주거": "Interior & Living: interior, home, housing, apartment, furniture, decoration, home design",
    "반려동물": "Pets: pet, dog, puppy, cat, kitten, pet care, animal, companion animal",
    "취미 & 라이프스타일": "Hobbies & Lifestyle: hobby, crafting, craft, diy, gardening, plant, collecting, leisure, lifestyle",
    "사진 & 영상": "Photography & Videography: photo, photography, picture, camera, filming, video, videography, shoot",
    "문구 & 디자인": "Stationery & Design: stationery, pen, paper, notebook, illustration, graphic design, drawing, sketch, typography",
    "비즈니스 & 스타트업": "Business & Startups: business, startup, entrepreneurship, entrepreneur, marketing, sales, management, company, corporate",
    "SNS & 커뮤니티": "SNS & Communities: social media, sns, facebook, instagram, twitter, tiktok, linkedin, chat, forum, community, post, profile, messaging",
    "여행 음식": "Travel Food: local food, street food, world cuisine, traditional food, regional dishes",
    "재테크": "Personal Finance: personal finance, saving, savings, side job, income, passive income, budgeting, money management",
    "자기계발 & 목표관리": "Self-Development & Goal Management: self development, self-improvement, productivity, habit, habit building, goal, planning, motivation"
}

# ===== 2. 직접 매칭 키워드 =====
tag_keywords = {
    "문화 & 예술": ["culture", "art", "museum", "painting", "literature", "exhibition", "theater", "festival"],
    "음식 & 요리": ["cooking", "recipe", "chef", "ingredient", "cuisine", "meal", "ramen", "sushi", "pizza"],
    "디저트 & 음료": ["coffee", "tea", "cake", "dessert", "latte", "smoothie", "bread", "chocolate"],
    "여행 & 관광": ["travel", "trip", "tour", "destination", "beach", "mountain", "hiking", "sightseeing"],
    "기술 & IT": ["technology", "software", "hardware", "AI", "robot", "coding", "programming", "gadget"],
    "모바일 & 앱": ["app", "smartphone", "android", "ios", "ux", "ui", "application"],
    "스포츠": ["soccer", "basketball", "tennis", "fitness", "yoga", "swimming", "baseball", "running"],
    "건강 & 웰빙": ["health", "diet", "meditation", "wellness", "exercise", "workout"],
    "패션 & 뷰티": ["fashion", "style", "makeup", "cosmetics", "skincare", "clothing"],
    "경제 & 금융": ["economy", "stock", "investment", "finance", "real estate", "market", "trading"],
    "교육 & 학습": ["education", "study", "school", "learning", "course", "lecture"],
    "엔터테인먼트": ["movie", "drama", "anime", "concert", "show", "cinema"],
    "게임": ["game", "console", "pc game", "esports", "playstation", "nintendo", "mobile game"],
    "사회 & 정치": ["politics", "government", "law", "policy", "society", "election"],
    "과학": ["science", "physics", "chemistry", "biology", "astronomy", "experiment"],
    "환경 & 자연": ["environment", "climate", "nature", "wildlife", "recycling", "sustainability"],
    "음악": ["music", "song", "instrument", "band", "concert", "album"],
    "언어 & 문화교류": ["language", "exchange", "translation", "bilingual", "learning"],
    "역사": ["history", "war", "ancient", "historical", "heritage", "monument"],
    "자동차 & 모빌리티": ["car", "automobile", "bike", "transport", "drive", "vehicle"],
    "인테리어 & 주거": ["interior", "furniture", "home", "apartment", "design", "decoration"],
    "반려동물": ["pet", "dog", "cat", "puppy", "kitten", "animal"],
    "취미 & 라이프스타일": ["hobby", "craft", "diy", "gardening", "collecting", "leisure"],
    "사진 & 영상": ["photography", "video", "camera", "filming", "shooting"],
    "문구 & 디자인": ["stationery", "illustration", "design", "drawing", "sketch"],
    "비즈니스 & 스타트업": ["startup", "business", "entrepreneur", "marketing", "sales", "management"],
    "SNS & 커뮤니티": ["facebook", "instagram", "twitter", "social", "sns", "community"],
    "여행 음식": ["local food", "street food", "traditional food", "regional cuisine"],
    "재테크": ["saving", "side job", "income", "budget", "asset"],
    "자기계발 & 목표관리": ["productivity", "habit", "goal", "planning", "motivation"]
}

model = SentenceTransformer("all-MiniLM-L6-v2")
tag_embeddings = {tag: model.encode(desc, convert_to_tensor=True)
                  for tag, desc in tag_descriptions.items()}

class ClassifyRequest(BaseModel):
    words: List[str]

class ClassifyResponse(BaseModel):
    scores: Dict[str, float]

def classify_word(word: str, threshold=0.2, top_k=3):
    for tag, keywords in tag_keywords.items():
        if word.lower() in [kw.lower() for kw in keywords]:
            return [(tag, 1.0)]
    import torch
    wv = model.encode(word, convert_to_tensor=True)
    sims = [(tag, util.cos_sim(wv, tv).item()) for tag, tv in tag_embeddings.items()]
    sims.sort(key=lambda x: x[1], reverse=True)
    weights = [1.0, 0.5, 0.25]
    out = []
    for i, (tag, s) in enumerate(sims[:top_k]):
        if s >= threshold:
            out.append((tag, s * weights[i]))
    return out

@app.post("/classify", response_model=ClassifyResponse)
def classify(req: ClassifyRequest):
    agg = {tag: 0.0 for tag in tag_descriptions.keys()}
    for w in req.words:
        for tag, sc in classify_word(w):
            agg[tag] += sc
    agg = {k: round(v, 4) for k, v in agg.items() if v > 0}
    return ClassifyResponse(scores=agg)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)

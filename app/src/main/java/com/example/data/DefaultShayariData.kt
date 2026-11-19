package com.example.data

object DefaultShayariData {
  // Primary Aegis Enterprise Console SDK config endpoint
  const val DEFAULT_REMOTE_CONFIG_URL =
    "https://aegis-enterprise-console.deepakdeepakbamniya92.workers.dev/api/v1/sdk/config"

  // Aegis App identifier sent via X-App-Id header
  const val AEGIS_APP_ID = "app_7d74c0634e9a6550f94ba47b"

  // Placeholder sentinel matching .env.example when secret has not been configured yet
  internal const val PLACEHOLDER_AEGIS_API_KEY = "YOUR_AEGIS_API_KEY"

  // Ordered endpoints for Aegis SDK config fetching
  val REMOTE_CONFIG_ENDPOINTS = listOf(DEFAULT_REMOTE_CONFIG_URL)

  // Bundled starter config matching the live Admin Panel config.json 1:1
  val DEFAULT_CONFIG_JSON =
    """
    {
      "app_name": "Shayari Lok",
      "app_logo_url": "",
      "latest_version_code": 1,
      "force_update_url": "https://play.google.com/store/apps/details?id=com.cyberking.shayarilok",
      "maintenance_mode": false,
      "maintenance_message": "हम ऐप को और बेहतर बना रहे हैं। कृपया कुछ समय बाद पुनः प्रयास करें।",
      "privacy_policy_url": "https://hackerdidigital-wq.github.io/ShayariLok-Admin-/privacy.html",
      "privacy_policy_text": "",
      "social_link": "https://t.me/shayarilok_official",
      "ads_enabled": true,
      "admob_app_id": "ca-app-pub-3940256099942544~3347511713",
      "banner_ad_id": "ca-app-pub-3940256099942544/6300978111",
      "interstitial_ad_id": "ca-app-pub-3940256099942544/1033173712",
      "native_ad_id": "",
      "dynamic_theme": {
        "primary_color": "#7B1CFA",
        "accent_color": "#FF2E93",
        "dark_mode": true,
        "card_style": "royal_glass"
      },
      "announcement_title": "नई हिंदी शायरी संग्रह लाइव है",
      "announcement_message": "आज ही Love और Motivation कैटेगरी में नई चुनिंदा शायरियाँ पढ़ें और दोस्तों के साथ शेयर करें।",
      "notifications": [
        {
          "id": 1,
          "title": "आज की ख़ास शायरी",
          "message": "दिल को छू लेने वाली नई मोहब्बत शायरियाँ जोड़ दी गई हैं।",
          "badge": "Shayari Alert",
          "time_label": "Today"
        },
        {
          "id": 2,
          "title": "Shayari Lok v1.0 Live",
          "message": "वन-टैप कॉपी और WhatsApp शेयर फ़ीचर के साथ नया अपडेट उपलब्ध है।",
          "badge": "App Info",
          "time_label": "Pinned"
        }
      ],
      "faqs": [
        {
          "id": "faq_1",
          "category": "Account & ID",
          "question": "How is my unique @SL_ ID generated?",
          "answer": "Your unique @SL_ handle is automatically generated from your signed-in Google email so friends can discover and message you securely across devices."
        },
        {
          "id": "faq_2",
          "category": "Profile",
          "question": "How do I update my photo, Bio, City, and Address?",
          "answer": "Open the Profile tab, tap Edit Profile, pick a photo straight from your phone gallery, fill in your Bio, City, and Address, and tap Save Profile."
        },
        {
          "id": "faq_3",
          "category": "Chat & Friends",
          "question": "How can I find a friend and send a private SMS/chat?",
          "answer": "Go to the Friends tab, search by your friend's @SL_ ID, name, or city, and tap Follow or Chat to start an instant private 1-on-1 conversation."
        },
        {
          "id": "faq_4",
          "category": "Appearance",
          "question": "Can I switch between Dark Theme and Light Theme?",
          "answer": "Yes! Open the side menu or Settings & Info to switch between Royal Dark Theme and Light Theme anytime."
        }
      ],
      "banners": [
        {
          "id": "banner_community",
          "badge": "FEATURED",
          "title": "Join Shayari Lok Official Club",
          "subtitle": "Daily fresh Shayari drops, festival specials & community updates.",
          "link_url": "https://t.me/shayarilok_official",
          "cta_text": "Explore Now"
        }
      ],
      "categories": [
        "All",
        "Love",
        "Attitude",
        "Motivation",
        "Sad"
      ],
      "shayari_list": [
        {
          "id": 1,
          "category": "Love",
          "text": "तेरी ख़ामोशी भी मुझसे कुछ कहती है,\nहर धड़कन में तेरी ही याद रहती है।",
          "author": "अज्ञात"
        },
        {
          "id": 2,
          "category": "Attitude",
          "text": "हम वो नहीं जो वक़्त के साथ बदल जाएं,\nहम वो हैं जो वक़्त का रुख मोड़ कर दिखाएं।",
          "author": "CyberKing"
        },
        {
          "id": 3,
          "category": "Motivation",
          "text": "मंज़िलें उन्हीं को मिलती हैं जिनके सपनों में जान होती है,\nपंखों से कुछ नहीं होता, हौसलों से उड़ान होती है।",
          "author": "रफ़्तार"
        },
        {
          "id": 4,
          "category": "Sad",
          "text": "कुछ दर्द ऐसे भी होते हैं जो बयाँ नहीं होते,\nबस आँखों की नमी में चुपचाप बह जाते हैं।",
          "author": "राही"
        },
        {
          "id": 5,
          "category": "Love",
          "text": "Kabhi lafzon mein na dhoondhna wafa meri,\nKhamoshiyon mein bhi sirf tera hi naam likha hai.",
          "author": "Shayari Lok"
        }
      ]
    }
    """.trimIndent()
}

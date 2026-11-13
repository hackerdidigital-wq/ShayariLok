package com.example.data

object DefaultShayariData {
  // Replace this URL with your own GitHub Raw config.json URL when publishing.
  const val DEFAULT_REMOTE_CONFIG_URL =
    "https://raw.githubusercontent.com/cyberking-dev/shayarilok-config/main/config.json"

  // Bundled starter config so first-time offline launches work immediately without login.
  val DEFAULT_CONFIG_JSON =
    """
    {
      "app_name": "Shayari Lok",
      "latest_version_code": 1,
      "force_update_url": "https://play.google.com/store/apps/details?id=com.cyberking.shayarilok",
      "maintenance_mode": false,
      "maintenance_message": "Hum naye lafzon ke saath wapas aa rahe hain. Kripya thodi der baad koshish karein.",
      "privacy_policy_url": "https://cyberking-dev.github.io/shayarilok-config/privacy-policy.html",
      "social_link": "https://t.me/shayarilok_official",
      "ads_enabled": false,
      "banner_ad_id": "",
      "interstitial_ad_id": "",
      "announcement_title": "Nayi Mehfil Update",
      "announcement_message": "Shayari Lok mein aapka swagat hai! Har roz nayi Hindi aur Hinglish shayari bina kisi login ke padhein aur share karein.",
      "notifications": [
        {
          "id": 1,
          "title": "Nayi Mehfil Collection Live",
          "message": "Love, Attitude, Motivation aur Sad categories mein nayi shayari jodi gayi hai. Offline padhne ke liye dil (Heart) icon se save karein.",
          "badge": "Shayari Alert",
          "time_label": "Today"
        },
        {
          "id": 2,
          "title": "Ultra-Lightweight Offline Ready",
          "message": "Aapki last synced shayari bina internet ke bhi hamesha uplabdh rehti hai. Nayi updates ke liye Sync button dabayein.",
          "badge": "App Info",
          "time_label": "Pinned"
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
          "id": 101,
          "category": "Love",
          "text": "तेरी मुस्कान ही मेरी पहचान बन गई,\nतेरी खुशी ही मेरी जान बन गई,\nकुछ इस कदर बसाया तुझे दिल में,\nकि तेरी याद ही मेरी सुबह-शाम बन गई।",
          "author": "Shayari Lok"
        },
        {
          "id": 102,
          "category": "Love",
          "text": "Kuch alfaaz bina kahe hi samajh liye jaate hain,\nKuch rishte bina sharton ke hi nibhaaye jaate hain.",
          "author": "Dil Se"
        },
        {
          "id": 103,
          "category": "Attitude",
          "text": "हम अपने अंदाज़ में ही मस्त रहते हैं,\nदूसरों की नकल करना हमारी फितरत में नहीं,\nवक्त आने पर जवाब खामोशी से देते हैं,\nबेवजह शोर मचाना हमारी आदत में नहीं।",
          "author": "Shayari Lok"
        },
        {
          "id": 104,
          "category": "Attitude",
          "text": "Apni sharton par jeena seekho,\nDuniya ki bheed toh hamesha raasta bhatkaati hai.",
          "author": "Royal Thoughts"
        },
        {
          "id": 105,
          "category": "Motivation",
          "text": "मंज़िलें उन्हीं को मिलती हैं,\nजिनके सपनों में जान होती है,\nसिर्फ पंखों से कुछ नहीं होता,\nहौसलों से ही असली उड़ान होती है।",
          "author": "Prerna"
        },
        {
          "id": 106,
          "category": "Motivation",
          "text": "Raat chahe jitni bhi gehri ho,\nSubah ki pehli kiran usse hara hi deti hai.\nBas apni mehnat par bharosa rakho.",
          "author": "Shayari Lok"
        },
        {
          "id": 107,
          "category": "Sad",
          "text": "खामोशियां भी बहुत कुछ कह जाती हैं,\nबस उन्हें सुनने वाला कोई अपना होना चाहिए।",
          "author": "Tanhaai"
        },
        {
          "id": 108,
          "category": "Sad",
          "text": "Waqt ne sikhaya hai chup rehna,\nVarna baatein toh hamare paas bhi hazaar thin.",
          "author": "Ehsaas"
        },
        {
          "id": 109,
          "category": "Love",
          "text": "चाँद को गुरूर है अपनी चाँदनी पर,\nऔर हमें गुरूर है तुम्हारी सादगी पर।",
          "author": "Shayari Lok"
        },
        {
          "id": 110,
          "category": "Motivation",
          "text": "हर छोटा बदलाव एक बड़ी कामयाबी की शुरुआत होता है,\nआज का एक कदम कल की मंज़िल तय करता है।",
          "author": "Udaan"
        }
      ]
    }
    """.trimIndent()
}

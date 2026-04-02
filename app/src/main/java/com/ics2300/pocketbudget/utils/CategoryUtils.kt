package com.ics2300.pocketbudget.utils

import android.graphics.Color
import com.ics2300.pocketbudget.R

import com.ics2300.pocketbudget.data.CategoryEntity

object CategoryUtils {

    fun getDefaultCategories(): List<CategoryEntity> {
        return listOf(
            CategoryEntity(name = "Uncategorized", keywords = "UNCATEGORIZED", colorHex = getDefaultColorHex(0)),
            CategoryEntity(name = "Food & Dining", keywords = "HOTEL,CAFE,RESTAURANT,JAVA,KFC,PIZZA,BURGER,CHICKEN,INN,GALITOS,PIZZA INN,CREAMY INN,SUBWAY,ARTCAFFE,BIG SQUARE,SIMBA SALOON,CARNIVORE,MAMA ASHANTI,CATERING,KIBANDA,EATERY,DINING,DELI,BAKERY,CAKES,SWEET,DESSERT", colorHex = getDefaultColorHex(1)),
            CategoryEntity(name = "Groceries", keywords = "SUPERMARKET,MART,NAIVAS,QUICKMART,CARREFOUR,CHANDARANA,TUSKYS,UCHUMI,SHOPRITE,GLACIER,CLEAN SHELF,MULLYS,MAGUNAS,PROVISIONS,VEGETABLES,MAMA MBOGA,GREEN GROCER,BUTCHERY,MEAT,MILK,DAIRY", colorHex = getDefaultColorHex(2)),
            CategoryEntity(name = "Transport", keywords = "UBER,BOLT,MATATU,SHELL,TOTAL,RUBIS,PETROL,STATION,GAS,FARAS,LITTLE CAB,EASY COACH,MASH,DREAMLINE,MODERN COAST,TAHSMEED,GUARDIAN,SWVL,PARKING,GARAGE,AUTO,SPARE,MECHANIC,TYRE,SERVICE,CAB,TAXIFY,BODA,MOTOR", colorHex = getDefaultColorHex(3)),
            CategoryEntity(name = "Utilities & Bills", keywords = "KPLC,TOKEN,ZUKU,SAFARICOM,AIRTEL,INTERNET,WIFI,POWER,TELKOM,FAIBA,LIQUID,DSTV,GOTV,STARTIMES,WATER,SEWERAGE,NAIROBI WATER,PREPAID,POSTPAID,KRA,TAX,COUNCIL,LICENSE,PERMIT,BILL,SUBSCRIPTION", colorHex = getDefaultColorHex(4)),
            CategoryEntity(name = "Entertainment", keywords = "NETFLIX,CINEMA,MOVIE,SHOWMAX,SPOTIFY,YOUTUBE,BET,GAMING,XBOX,PLAYSTATION,STEAM,NINTENDO,PUB,LOUNGE,BAR,CLUB,TICKET,EVENT,CONCERT,PARTY,DRINKS,WINES,SPIRITS,LIQUOR", colorHex = getDefaultColorHex(5)),
            CategoryEntity(name = "Shopping", keywords = "CLOTHING,MALL,FASHION,STORE,SHOP,JUMIA,KILIMALL,AMAZON,SHEIN,ALIBABA,ALIEXPRESS,ADIDAS,NIKE,ZARA,H&M,DECATHLON,PIGIA ME,ELECTRONICS,MOBILE,PHONE,LAPTOP,GADGET,WEAR,BOUTIQUE,COSMETICS,BEAUTY,SALON,BARBER,SPA", colorHex = getDefaultColorHex(6)),
            CategoryEntity(name = "Health & Wellness", keywords = "HOSPITAL,CHEMIST,PHARMACY,DOCTOR,CLINIC,MEDIC,DENTAL,OPTICAL,NHIF,SHA,AAR,OLD MUTUAL,BRITAM,JUBILEE,GETRUDES,MP SHAH,AGA KHAN,KAREN HOSPITAL,MATERNITY,LAB,GYM,FITNESS,WELLNESS,SPA,THERAPY", colorHex = getDefaultColorHex(7)),
            CategoryEntity(name = "Rent & Home", keywords = "RENT,LANDLORD,HOUSING,ESTATE,APARTMENT,REALTY,MORTGAGE,SERVICE CHARGE,FURNITURE,HARDWARE,CONSTRUCTION,PAINT,ELECTRICAL,PLUMBING,CLEANING,LAUNDRY,HOME,DECOR,MESS,HOUSE", colorHex = getDefaultColorHex(8)),
            CategoryEntity(name = "Education", keywords = "SCHOOL,COLLEGE,UNIVERSITY,FEES,TUITION,ACADEMY,KINDERGARTEN,UDEMY,COURSERA,EDX,KHAN ACADEMY,STRATHMORE,USIU,UON,KU,JKUAT,BOOKS,STATIONERY,LIBRARY,EXAM,COUNCIL", colorHex = getDefaultColorHex(9)),
            CategoryEntity(name = "Transfer & Cash", keywords = "SENT,TRANSFER,MPESA,POCHI,LA BIASHARA,TILL,PAYBILL,WESTERN UNION,MONEYGRAM,REMITLY,WORLDREMIT,WITHDRAW,AGENT,ATM,CASH,M-SHWARI,DEPOSIT,KCB,COOP,EQUITY,ABSA,STANCHART,NCBA,FAMILY BANK,I&M,DTB,BANK", colorHex = getDefaultColorHex(10)),
            CategoryEntity(name = "Income", keywords = "RECEIVED,DEPOSIT,SALARY,DIVIDEND,INTEREST,REFUND,COMMISSION,BONUS,STIPEND,PAYOUT,CASHBACK,GIFT,REWARD", colorHex = getDefaultColorHex(11))
        )
    }

    val availableColors = listOf(
        "#0A3D2E", // Brand Dark Green
        "#D4E157", // Brand Light Green
        "#F44336", // Red
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#673AB7", // Deep Purple
        "#3F51B5", // Indigo
        "#2196F3", // Blue
        "#03A9F4", // Light Blue
        "#00BCD4", // Cyan
        "#009688", // Teal
        "#4CAF50", // Green
        "#8BC34A", // Light Green
        "#CDDC39", // Lime
        "#FFEB3B", // Yellow
        "#FFC107", // Amber
        "#FF9800", // Orange
        "#FF5722", // Deep Orange
        "#795548", // Brown
        "#9E9E9E", // Grey
        "#607D8B"  // Blue Grey
    )

    // Using standard android drawables as placeholders for now since we don't have custom assets
    // In a real app, these would be R.drawable.ic_category_food, etc.
    val iconMap = mapOf(
        "ic_default" to android.R.drawable.ic_menu_my_calendar,
        "ic_food" to android.R.drawable.ic_menu_my_calendar, // Placeholder
        "ic_transport" to android.R.drawable.ic_menu_directions,
        "ic_shopping" to android.R.drawable.ic_menu_gallery,
        "ic_home" to android.R.drawable.ic_menu_manage, // Using settings icon for home/utilities
        "ic_bills" to android.R.drawable.ic_menu_agenda,
        "ic_health" to android.R.drawable.ic_menu_add,
        "ic_entertainment" to android.R.drawable.ic_menu_camera,
        "ic_education" to android.R.drawable.ic_menu_edit,
        "ic_savings" to android.R.drawable.ic_menu_save,
        "ic_other" to android.R.drawable.ic_menu_help
    )

    fun getIconResId(iconName: String): Int {
        return iconMap[iconName] ?: android.R.drawable.ic_menu_my_calendar
    }

    fun getColor(colorHex: String): Int {
        return try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            Color.parseColor("#0A3D2E") // Default
        }
    }

    fun getDefaultColorHex(index: Int): String {
        return availableColors[index % availableColors.size]
    }
}

# Bendrabučių skalbyklų rezervavimo sistema

Kursinio darbo I dalis: projektavimo dokumentas


## 1. Problema ir idėja

**Sistema vienu sakiniu:** Sistema skirta bendrabučių gyventojams peržiūrėti savo bendrabučio skalbimo mašinų prieinamumą ir rezervuoti pasirinktą skalbimo mašiną konkrečiam laikui, išvengiant persidengiančių rezervacijų.

**Problema ir dabartinis procesas:** Šiame projekte modeliuojama situacija, kai bendrabutyje yra kelios bendro naudojimo skalbimo mašinos, tačiau nėra centralizuotos jų rezervavimo sistemos. Tokiu atveju gyventojas gali tik atėjęs į skalbyklą sužinoti, ar norima skalbimo mašina yra laisva, arba laiką neformaliai derinti su kitais gyventojais. Dėl to sunkiau iš anksto suplanuoti skalbimą, o keli gyventojai gali norėti naudotis ta pačia skalbimo mašina tuo pačiu metu. Sistema spręs šią problemą leisdama iš anksto matyti mašinų prieinamumą ir neleisdama sukurti konfliktuojančių rezervacijų.

**Nauda:** Naudotojas galės iš anksto pasirinkti tinkamą skalbimo mašiną ir laisvą laiką, matyti savo būsimas rezervacijas bei atšaukti nebereikalingą rezervaciją. Sistema pati tikrins rezervavimo taisykles, todėl bus išvengiama situacijų, kai tai pačiai skalbimo mašinai tuo pačiu metu sukuriamos kelios rezervacijos.

**Naudotojai:** Pagrindinis sistemos naudotojas yra bendrabučio gyventojas. Jis galės peržiūrėti savo bendrabučio skalbimo mašinas ir jų prieinamumą, sukurti rezervaciją, peržiūrėti savo rezervacijas ir jas atšaukti. Vėlesniame sistemos etape taip pat numatoma bendrabučio administratoriaus rolė, skirta savo bendrabučio skalbimo mašinoms ir rezervavimo nustatymams valdyti.

**Prielaidos:** Daroma prielaida, kad sistema bus naudojama keliuose bendrabučiuose, o kiekvienas bendrabutis sistemoje bus laikomas atskiru klientu. Kiekvienas naudotojas ir kiekviena skalbimo mašina priklausys konkrečiam bendrabučiui, o vieno bendrabučio naudotojai negalės pasiekti kito bendrabučio duomenų. Pradiniame variante daroma prielaida, kad viena rezervacija trunka 60 minučių, o skalbykla veikia nuo 07:00 iki 23:00. Vėlesniame etape rezervavimo nustatymai galės būti skirtingi kiekvienam bendrabučiui.

## 2. Apimtis

| Funkcija | Ką naudotojas galės atlikti | Pagrindinis modulis ar pagalbinė funkcija |
|---|---|---|
| Skalbimo mašinų prieinamumo peržiūra | Veiksmas: naudotojas pasirenka skalbimo mašiną ir datą. Rezultatas: sistema parodo laisvus rezervavimo laikus | Pagalbinė funkcija |
| Rezervacijos kūrimas | Veiksmas: naudotojas pasirenka skalbimo mašiną, datą ir laiką bei pateikia rezervacijos prašymą. Rezultatas: sistema sukuria rezervaciją arba pateikia jos atmetimo priežastį | Pagrindinis modulis |
| Mano rezervacijų peržiūra | Veiksmas: naudotojas atidaro savo rezervacijų sąrašą. Rezultatas: sistema parodo jo būsimas rezervacijas | Pagalbinė funkcija |
| Rezervacijos atšaukimas | Veiksmas: naudotojas pasirenka būsimą rezervaciją ir ją atšaukia. Rezultatas: rezervacija atšaukiama, o jos laikas vėl tampa prieinamas | Pagalbinė funkcija |

**Į kursinio darbo apimtį neįeina:** Mokėjimų funkcionalumas, SMS, el. pašto ir mobilieji pranešimai, fizinis ryšys su skalbimo mašinomis, gedimų diagnostika, statistikos ir ataskaitų sistema, automatinis alternatyvaus rezervacijos laiko parinkimas, mobilioji aplikacija, savitarnos naujų bendrabučių registravimas ir mikroservisų architektūra.

## 3. Pagrindinis modulis

**Pavadinimas ir atsakomybė:** Rezervacijos kūrimo modulis. Jo atsakomybė – įvertinti naudotojo pateiktą rezervacijos prašymą ir, jei visos rezervavimo taisyklės tenkinamos, sukurti rezervaciją. Jei bent viena taisyklė netenkinama, rezervacija nesukuriama ir pateikiama konkreti atmetimo priežastis.

**Logika, kurią reikės projektuoti ir testuoti:** Prieš sukuriant rezervaciją sistema turės atlikti kelis atskirus patikrinimus: nustatyti, ar pasirinkta skalbimo mašina priklauso tam pačiam bendrabučiui kaip naudotojas, patikrinti rezervacijos laiką, skalbyklos darbo valandas, pasirinktos skalbimo mašinos užimtumą ir naudotojo turimų rezervacijų persidengimą. Kiekvienas patikrinimas bus atskira pagrindinio modulio logikos dalis.

**Įvestis:** Naudotojo kontekstas, pasirinkta skalbimo mašina, rezervacijos data ir pradžios laikas. Pavyzdžiui: naudotojas – Studentas A, priklausantis Bendrabučiui A; skalbimo mašina – Nr. 2; data – 2026-11-05; pradžios laikas – 15:00. Pradiniame variante viena rezervacija trunka 60 minučių, todėl jos pabaigos laikas būtų 16:00.

**Išvestis:** Sėkmės atveju sukuriama rezervacija ir pateikiamas patvirtinimas, pavyzdžiui: „Rezervacija sėkmingai sukurta: skalbimo mašina Nr. 2, 2026-11-05, 15:00–16:00.“ Nesėkmės atveju rezervacija nesukuriama ir pateikiama konkreti priežastis, pavyzdžiui: „Pasirinktas laikas jau užimtas.“

**Veikimo eiga:** 1. Naudotojas pasirenka skalbimo mašiną, datą ir laiką. 2. Sistema nustato naudotojo bendrabutį ir patikrina pasirinktos skalbimo mašinos priklausomybę. 3. Patikrinama, ar rezervacijos laikas tinkamas ir patenka į skalbyklos darbo laiką. 4. Patikrinama, ar pasirinkta skalbimo mašina tuo metu nėra rezervuota. 5. Patikrinama, ar pats naudotojas tuo metu neturi kitos rezervacijos. 6. Jei visi patikrinimai sėkmingi, rezervacija sukuriama. 7. Jei bent vienas patikrinimas nesėkmingas, rezervacija nesukuriama ir pateikiama atmetimo priežastis.

### Taisyklės arba sprendimo žingsniai

1. Naudotojas gali rezervuoti tik tam pačiam bendrabučiui, kuriam jis priklauso, priskirtą skalbimo mašiną.
2. Rezervacijos pradžios laikas turi būti ateityje, o visas rezervacijos laikas turi patekti į nustatytas skalbyklos darbo valandas.
3. Tai pačiai skalbimo mašinai negali būti sukurtos laiku persidengiančios rezervacijos. Pavyzdžiui, jei mašina rezervuota 15:00–16:00, rezervacija 15:30–16:30 negalima, tačiau 16:00–17:00 galima.
4. Tas pats naudotojas negali tuo pačiu metu turėti dviejų persidengiančių skalbimo mašinų rezervacijų.


### Scenarijai būsimiems testams

| Scenarijus | Pradinės sąlygos ir konkreti įvestis | Veiksmas | Tikslus laukiamas rezultatas |
|---|---|---|---|
| Įprastas atvejis | Studentas A ir skalbimo mašina Nr. 2 priklauso Bendrabučiui A. 2026-11-05 15:00–16:00 mašina yra laisva, o Studentas A tuo metu kitos rezervacijos neturi. | Studentas A pateikia rezervacijos prašymą mašinai Nr. 2 2026-11-05 15:00. | Sukuriama rezervacija 2026-11-05 15:00–16:00 ir pateikiamas sėkmingas rezervacijos patvirtinimas. |
| Ribinis atvejis arba konfliktas | Skalbimo mašina Nr. 2 Bendrabutyje A jau rezervuota 2026-11-05 15:00–16:00. | Studentas A bando rezervuoti tą pačią mašiną nuo 15:30. | Rezervacija nesukuriama, nes intervalas 15:30–16:30 persidengia su jau esančia rezervacija. |
| Klaida arba neįmanomas rezultatas | Studentas A priklauso Bendrabučiui A, o skalbimo mašina Nr. 5 priklauso Bendrabučiui B. | Studentas A bando rezervuoti skalbimo mašiną Nr. 5. | Rezervacija nesukuriama, nes naudotojas negali rezervuoti kitam bendrabučiui priklausančios skalbimo mašinos. |

**Jei modulis naudoja AI:** Netaikoma.

## 4. Kokybės atributas

**Pasirinktas atributas:** Palaikomumas.

**Kodėl svarbus šiai sistemai:** Sistema bus naudojama keliuose bendrabučiuose, todėl ateityje gali keistis atskirų bendrabučių rezervavimo nustatymai. Svarbu, kad tokius pakeitimus būtų galima atlikti neperrašant pagrindinės rezervacijų kūrimo logikos.

**Tikrinimo scenarijus ir sąlygos:** Pradiniame variante Bendrabučio A ir Bendrabučio B rezervacijos trukmė yra 60 minučių. Tikrinimo metu Bendrabučio A rezervacijos trukmė pakeičiama į 90 minučių, o Bendrabučio B paliekama 60 minučių.

**Sėkmės kriterijus:** Po pakeitimo Bendrabutyje A naujos rezervacijos trunka 90 minučių, o Bendrabutyje B – 60 minučių. Pagrindinės rezervacijų konfliktų tikrinimo logikos nereikia dubliuoti ar perrašyti, o esami automatiniai testai išlieka sėkmingi.

**Numatytas projektavimo sprendimas:** Rezervacijų kūrimo verslo logika bus atskirta nuo naudotojo sąsajos ir duomenų saugojimo. Kiekvieno bendrabučio rezervavimo nustatymai bus saugomi atskirai, o rezervacijų kūrimo modulis naudos konkretaus bendrabučio nustatymus.

**Kaip patikrinsiu vėlesniame etape:** Pakeisiu Bendrabučio A rezervacijos trukmės nustatymą ir automatiniais testais patikrinsiu, ar Bendrabučio A ir Bendrabučio B rezervacijos kuriamos pagal skirtingus nustatymus ir ar esami rezervavimo taisyklių testai vis dar sėkmingi.

**Sprendimo kaina arba ribojimas:** Reikės papildomai saugoti ir perduoti kiekvieno bendrabučio rezervavimo nustatymus, todėl sistemos struktūra bus šiek tiek sudėtingesnė nei sistemai, skirtai tik vienam bendrabučiui.


## 5. Pradinė sistemos struktūra

### Paprasta schema

```text
Naudotojas
↓
Naudotojo sąsaja / API
↓
Rezervacijų kūrimo verslo logika
↓
Duomenų prieigos dalis
↓
Reliacinė duomenų bazė
```


| Sistemos dalis | Atsakomybė |
|---|---|
| Naudotojo sąsaja / API | Priimti naudotojo veiksmus ir perduoti duomenis pagrindinei sistemos logikai bei pateikti rezultatą naudotojui. |
| Rezervacijų kūrimo verslo logika | Tikrinti rezervavimo taisykles ir nuspręsti, ar rezervacija gali būti sukurta. |
| Duomenų prieigos dalis | Gauti ir išsaugoti naudotojų, bendrabučių, skalbimo mašinų ir rezervacijų duomenis. |
| Reliacinė duomenų bazė | Saugoti bendrabučių, naudotojų, skalbimo mašinų, rezervacijų ir rezervavimo nustatymų duomenis. |


**Planuojamos technologijos ir pasirinkimo priežastys:** Pagrindinė programavimo kalba – Java, nes ji naudojama studijų metu ir tinka objektiniam programavimui bei verslo logikai realizuoti. Duomenys bus saugomi reliacinėje duomenų bazėje, naudojant SQL, nes sistemoje yra aiškiai susiję duomenys: bendrabučiai, naudotojai, skalbimo mašinos ir rezervacijos. Sistema bus projektuojama taip, kad pagrindinė verslo logika būtų atskirta nuo naudotojo sąsajos ir duomenų saugojimo. Taip pat numatoma stateless API struktūra, tinkama kelių klientų SaaS sistemai. Konkretus Java API karkasas, duomenų bazės valdymo sistema ir testavimo biblioteka bus pasirinkti prototipo kūrimo etape.

## 6. AI panaudojimas

### AI rengiant šį dokumentą

Taip, rengiant dokumentą buvo naudojamos AI priemonės.

| Priemonė ir užduotis | Ką panaudojau | Ką atmečiau arba perrašiau ir kodėl | Kaip patikrinau |
|---|---|---|---|
| ChatGPT – projekto idėjos, apimties, pagrindinio modulio, taisyklių, testavimo scenarijų ir sistemos struktūros aptarimas | Panaudojau pasiūlymus projekto struktūrai, rezervacijos kūrimo modulio taisyklėms, testavimo scenarijams ir dokumento formuluotėms. | Atmečiau arba supaprastinau sprendimus, kurie būtų per sudėtingi šio kursinio darbo apimčiai, bei nepasirinkau technologijų, kurių dar nesu nusprendęs naudoti. | Pasiūlymus palyginau su kursinio darbo užduotimi, pateiktu šablonu ir paskaitų medžiaga. Galutiniame variante palikau tik tai, ką suprantu ir galėčiau paaiškinti. |
| Gemini Pro – nepriklausomas parengto projekto plano patikrinimas | Panaudojau kaip antrą nepriklausomą patikrinimą, ar projekto planas yra nuoseklus ir nepraleidžia svarbių reikalavimų. | Jo pasiūlymų automatiškai neperkėliau. Galutinį variantą palikau tik tada, kai jis atitiko kurso reikalavimus ir mano pasirinktą projekto apimtį. | Gemini Pro pastabas palyginau su užduoties reikalavimais ir jau parengtu dokumentu, o galutinius sprendimus pasirinkau pats. |

### Planuojamas AI naudojimas kuriant sistemą

**Kur ir kam naudosiu AI:** AI planuoju naudoti kaip pagalbinę priemonę programuojant: aiškinantis neaiškias Java kodo vietas, ieškant klaidų priežasčių, rengiant automatinių testų pavyzdžius, peržiūrint kodą ir ieškant galimybių jį supaprastinti ar refaktorinti.

**Kaip tikrinsiu pasiūlymus ir sugeneruotą kodą:** Sugeneruotą kodą pirmiausia perskaitysiu ir įsitikinsiu, kad suprantu jo veikimą. Kodą paleisiu ir patikrinsiu konkrečiais testavimo scenarijais bei automatiniais testais. Taip pat tikrinsiu, ar sprendimas atitinka projekto reikalavimus ir neįveda nereikalingo sudėtingumo. Kodo, kurio negalėčiau paaiškinti, į projektą neįtrauksiu.

**Ar AI bus sistemos funkcionalumo dalis:** Ne. AI nebus sistemos funkcionalumo dalis.

## 7. Tolesnių darbų planas

| Darbas | Apčiuopiamas rezultatas | Planuojama darbų seka |
|---|---|---|
| Sukurti pagrindinį duomenų modelį | Parengti pagrindiniai sistemos objektai: bendrabutis, naudotojas, skalbimo mašina ir rezervacija bei jų tarpusavio ryšiai. | 1. Pirmiausia, nes šiais duomenimis remsis likusi sistemos logika. |
| Realizuoti rezervacijos kūrimo logiką | Veikiantis pagrindinis modulis, tikrinantis rezervavimo taisykles ir sukuriantis arba atmetantis rezervaciją. | 2. Po pagrindinio duomenų modelio sukūrimo. |
| Sukurti automatinius pagrindinio modulio testus | Testai įprastam atvejui, rezervacijos konfliktui, netinkamai rezervacijai ir skirtingų bendrabučių duomenų izoliavimui. | 3. Sukūrus pagrindinę rezervacijos logiką. |
| Prijungti duomenų saugojimą ir kelių bendrabučių duomenis | Duomenų bazėje saugomi bent dviejų bendrabučių naudotojų, skalbimo mašinų ir rezervacijų duomenys, atskirti pagal bendrabutį. | 4. Kai pagrindinė logika ir testai jau veikia. |
| Parengti paprastą veikiantį prototipą | Naudotojas gali pateikti rezervacijos prašymą ir gauti sėkmės arba atmetimo rezultatą per paprastą sąsają ar API. | 5. Sujungus pagrindinę logiką ir duomenų saugojimą. |

**Būsimo prototipo veikimo scenarijus:** Bendrabutyje A Studentas A pasirenka skalbimo mašiną Nr. 2 ir bando rezervuoti ją 2026-11-05 15:00. Jei laikas laisvas, sistema sukuria rezervaciją 15:00–16:00 ir pateikia patvirtinimą. Tada kitas rezervacijos prašymas tai pačiai mašinai nuo 15:30 turi būti atmestas dėl persidengiančio laiko. Taip pat bus parodyta, kad Bendrabučio A naudotojas negali pasiekti ar pakeisti Bendrabučio B rezervacijos.

| Rizika arba neaiškumas | Kaip patikrinsiu arba sumažinsiu |
|---|---|
| Rezervacijų laikų persidengimo taisyklė gali būti realizuota neteisingai. | Sukursiu automatinius testus persidengiantiems ir gretimiems intervalams, pavyzdžiui, 15:00–16:00 ir 16:00–17:00. |
| Gali būti netinkamai atskirti skirtingų bendrabučių duomenys. | Naudosiu bent dviejų bendrabučių testinius duomenis ir sukursiu testą, kuris patikrins, kad vieno bendrabučio naudotojas negali pasiekti ar keisti kito bendrabučio duomenų. |
| Projektas gali tapti per sudėtingas arba AI gali pasiūlyti nereikalingai sudėtingą sprendimą. | Laikysiuosi šiame dokumente apibrėžtos apimties, o AI pasiūlymus naudosiu tik tada, kai juos suprasiu ir galėsiu paaiškinti. |

## Šaltiniai, jei naudojote

1. „Programų sistemų projektavimas. Įvadinė paskaita“.
2. „Kokybiškas programinis kodas. Refaktorinimas“.

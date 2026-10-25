# Bendrabučių skalbyklų rezervavimo sistema

## Projekto aprašymas

Java konsolinė programa, skirta studentų skalbimo mašinų rezervacijoms bendrabučiuose valdyti.

Sistema leidžia kurti, peržiūrėti ir atšaukti rezervacijas. Taip pat tikrina rezervavimo taisykles ir atskiria skirtingų bendrabučių duomenis.

## Naudojamos technologijos

- Java 21
- Maven ir Maven Wrapper
- JUnit 5
- IntelliJ IDEA

Duomenys laikinai saugomi kompiuterio atmintyje, naudojant `ArrayList`. Duomenų bazė šiame etape nenaudojama.

## Programos paleidimas

Reikalinga įdiegta Java 21 arba naujesnė suderinama versija.

Atidarius Terminal projekto pagrindiniame aplanke, įvykdyti vieną komandą:

```bash
./mvnw compile exec:java -Dexec.mainClass=Main
```

Pirmą kartą paleidžiant gali reikėti interneto ryšio Maven priklausomybėms atsisiųsti. Atskirai įdiegti Maven nereikia.

Programai pasileidus, pateikiamas meniu:

1. Sukurti rezervaciją.
2. Peržiūrėti savo rezervacijas.
3. Atšaukti rezervaciją.
4. Pakeisti demonstracinį studentą.
0. Baigti programą.

## Automatiniai testai

Testai paleidžiami komanda:

```bash
./mvnw test
```

Projekte yra 15 automatinių JUnit testų, tikrinančių rezervavimo taisykles ir duomenų izoliavimą.

## Rezervavimo taisyklės

- Studentas gali rezervuoti tik savo bendrabučio skalbimo mašiną.
- Rezervacijos pradžia turi būti ateityje.
- Rezervacijos trukmė – 1 valanda.
- Rezervacijos turi tilpti į darbo laiką nuo 07:00 iki 23:00.
- Negalimos persidengiančios tos pačios mašinos rezervacijos.
- Studentas negali vienu metu rezervuoti skirtingų mašinų.
- Gretimos rezervacijos leidžiamos, pavyzdžiui, 15:00–16:00 ir 16:00–17:00.

## Demonstraciniai duomenys

Programoje sukurti du demonstraciniai studentai:

- Studentas A – bendrabutis Nr. 1.
- Studentas B – bendrabutis Nr. 2.

Kiekviename bendrabutyje yra skalbimo mašinos Nr. 2 ir Nr. 3.

Meniu pasirinkimas `4` leidžia demonstravimo metu pakeisti studentą. Tai nėra tikra naudotojų autentifikavimo sistema.

## Demonstravimo scenarijus

1. Studentas A rezervuoja mašiną Nr. 2, 2030-11-22 15:00.
2. Perjungiama į Studentą B. Jo rezervacijų sąrašas tuščias.
3. Studentas B rezervuoja savo bendrabučio mašiną Nr. 2 tuo pačiu laiku.
4. Perjungiama į Studentą A. Bandymas atšaukti Studento B rezervaciją atmetamas.
5. Studentas B vis dar mato savo rezervaciją.

Taip parodomas rezervacijų atskyrimas pagal bendrabutį.

## Projekto struktūra

- `Main.java` – konsolės meniu ir naudotojo įvestis.
- `ReservationService.java` – rezervavimo verslo logika.
- `ReservationRepository.java` – rezervacijų saugojimas.
- `Reservation.java` – rezervacijos duomenys.
- `User.java` – studento duomenys.
- `Dormitory.java` – bendrabučio duomenys.
- `WashingMachine.java` – skalbimo mašinos duomenys.
- `ReservationServiceTest.java` – rezervavimo logikos testai.
- `WashingMachineTest.java` – skalbimo mašinos testas.

## Apribojimai

Tai antrojo projekto etapo prototipas. Rezervacijos saugomos tik programos veikimo metu, todėl ją išjungus duomenys neišlieka. Duomenų bazė ir tikra prisijungimo sistema šiame etape neįgyvendintos.
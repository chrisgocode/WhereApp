# Where App

## Overview

Where App is a mobile application designed to simplify group meetups. It
helps friend groups effortlessly find convenient locations for gatherings by
considering individual preferences and optimizing for fair travel distances,
making social planning fun, efficient, and inclusive.

## Functionality

*   **User Authentication with Firebase Authentication:** Secure user
    registration and login using Firebase's authentication services.
*   **Profile Customization:** Personalized profiles for tailored restaurant
    recommendations, including:
    *   Profile Picture
    *   Interests (e.g., hiking, movies, board games)
    *   Favorite Cuisine (e.g., Italian, Mexican, Thai)
    *   Preferred Restaurant Vibe (e.g., Casual, Romantic, Lively)
    *   Dietary Restrictions (e.g. Vegetarian, Gluten-Free)
    *   Price Range
*   **Group Feature:** Create and manage groups to add friends and streamline
    the location finding process. Users can be part of multiple groups.
*   **Location Sharing:** Only initiated when a group meetup is started and used
    to calculate the distance between the users and the potential meetup
    location.

## Tech Stack

### Database

Firestore: Firestore is used for its ease of integration with the Android SDK
and its real-time data synchronization capabilities, ensuring a smooth user
experience.

### API

Google Maps API: Used to display interactive maps, generate routes, and
calculate travel distances between users and potential meetup locations.

Google Places API: Provides detailed information about venues, including
addresses, ratings, reviews, and contact details, enabling informed
decision-making.

### Sensors

GPS: Utilized to capture real-time user locations for accurate distance
calculations and optimized location suggestions.

### Target Devices

*   Mobile Devices
*   Tablet Devices

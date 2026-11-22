/*
 * The app's categories and which regions each one has.
 *
 * A category with only "world" is universal (no country picker).
 * A category with country regions shows a flag picker ("Movies from which country?").
 * Card lists live in content/<region>.js; India reuses the web game's js/data.js.
 *
 * mode: 'say' (describe it), 'act' (no talking), 'hum' (hum the tune), 'sound' (make the noise)
 */
module.exports = {
  regions: [
    { id: 'world', name: 'World', flag: '🌍' },
    { id: 'in', name: 'India', flag: '🇮🇳' },
    { id: 'us', name: 'USA', flag: '🇺🇸' },
    { id: 'gb', name: 'UK', flag: '🇬🇧' },
    { id: 'cn', name: 'China', flag: '🇨🇳' },
  ],

  // India decks in js/data.js, by category id
  india: {
    movies: 'bollywood-movies', stars: 'stars', act: 'act-it-out', festivals: 'festivals', weddings: 'shaadi',
    food: 'food', brands: 'brands', objects: 'ghar', sports: 'cricket', places: 'places', myths: 'mythology',
    tv: 'tv', lines: 'dialogues', songs: 'songs', life: 'desi-life',
  },

  // group: which home-screen shelf the category sits on (see `groups`).
  groups: [
    { id: 'party', desc: 'Describe it, but never say the word!', name: 'Party Classics' },
    { id: 'act', desc: 'No talking allowed!', name: 'Act It Out' },
    { id: 'pop', desc: 'Pick your country when you open a deck', name: 'Movies, Music & TV' },
    { id: 'family', desc: 'Easy and fun for all ages', name: 'Family Fun' },
    { id: 'culture', desc: 'Pick your country when you open a deck', name: 'Festivals & Traditions' },
  ],

  categories: [
    // ---- Party Classics (universal) ----
    { id: 'animals', group: 'party', name: 'Animals', emoji: '🐘', colors: ['#56ab2f', '#a8e063'], mode: 'say',
      desc: 'Animals from the farm, the jungle and the sea. Sounds and actions allowed!' },
    { id: 'objects', group: 'party', name: 'Everyday Things', emoji: '⏰', colors: ['#8e2de2', '#4a00e0'], mode: 'say',
      desc: 'Things you use every day, at home, at school and on the go.' },
    { id: 'food', group: 'party', name: 'Food & Drinks', emoji: '🍕', colors: ['#e65c00', '#f9d423'], mode: 'say',
      desc: 'Snacks, dishes and drinks. Warning: you will get hungry.' },
    { id: 'famous', group: 'party', name: 'Famous People', emoji: '👑', colors: ['#f7b733', '#fc4a1a'], mode: 'say',
      desc: 'Legends from history, science, art, sport and music.' },
    { id: 'jobs', group: 'party', name: 'Jobs', emoji: '🧑‍🚒', colors: ['#2193b0', '#6dd5ed'], mode: 'say',
      desc: 'What do they do all day? Describe or act out the job.' },
    { id: 'sports', group: 'party', name: 'Sports', emoji: '⚽', colors: ['#11998e', '#38ef7d'], mode: 'say',
      desc: 'Games, legends and famous moments.' },
    { id: 'places', group: 'party', name: 'Famous Places', emoji: '🗺️', colors: ['#f953c6', '#b91d73'], mode: 'say',
      desc: 'Landmarks, cities and wonders. Where in the world?' },
    { id: 'countries', group: 'party', name: 'Countries', emoji: '🌎', colors: ['#0575e6', '#00f260'], mode: 'say',
      desc: 'Food, flags, accents and landmarks: describe the country!' },
    { id: 'brands', group: 'party', name: 'Brands', emoji: '🛍️', colors: ['#4776e6', '#8e54e9'], mode: 'say',
      desc: 'Logos you know by heart. Sing the jingle if you dare!' },
    { id: 'vehicles', group: 'party', name: 'Things That Go', emoji: '🏎️', colors: ['#cb356b', '#bd3f32'], mode: 'say',
      desc: 'Cars, planes, boats and everything with an engine (or pedals).' },
    { id: 'hobbies', group: 'party', name: 'Hobbies', emoji: '🎨', colors: ['#ee9ca7', '#b06ab3'], mode: 'say',
      desc: 'What people love doing in their free time.' },
    // ---- Act It Out (universal) ----
    { id: 'act', group: 'act', name: 'Charades', emoji: '🎭', colors: ['#00b09b', '#96c93d'], mode: 'act',
      desc: 'No talking! Act out the action for the guesser.' },
    { id: 'emotions', group: 'act', name: 'Emotions', emoji: '😆', colors: ['#f12711', '#f5af19'], mode: 'act',
      desc: 'Show the feeling with your face and body. No words!' },
    { id: 'sounds', group: 'act', name: 'Make the Sound', emoji: '🔊', colors: ['#1d976c', '#93f9b9'], mode: 'sound',
      desc: 'Moo, beep, achoo! Make the noise, no words allowed.' },
    { id: 'instruments', group: 'act', name: 'Instruments', emoji: '🎸', colors: ['#e53935', '#e35d5b'], mode: 'act',
      desc: 'Pretend to play it. Humming the sound is allowed!' },
    { id: 'dance', group: 'act', name: 'Dance Moves', emoji: '🪩', colors: ['#c94b4b', '#4b134f'], mode: 'act',
      desc: 'Bust a move! Show the dance, no talking.' },
    // ---- Movies, Music & TV (pick a country) ----
    { id: 'movies', group: 'pop', name: 'Movies', emoji: '🎬', colors: ['#ff3d6e', '#ff9a3d'], mode: 'say',
      desc: 'Describe the story without saying the title!' },
    { id: 'stars', group: 'pop', name: 'Celebrities', emoji: '🌟', colors: ['#7b2ff7', '#f107a3'], mode: 'say',
      desc: 'Actors, singers and famous faces. Impressions allowed!' },
    { id: 'songs', group: 'pop', name: 'Songs', emoji: '🎤', colors: ['#fc466b', '#3f5efb'], mode: 'hum',
      desc: 'Hum the tune, no lyrics allowed!' },
    { id: 'tv', group: 'pop', name: 'TV Shows', emoji: '📺', colors: ['#3a1c71', '#ffaf7b'], mode: 'say',
      desc: 'Sitcoms, cartoons, reality shows and binge-worthy series.' },
    { id: 'lines', group: 'pop', name: 'Famous Movie Lines', emoji: '💬', colors: ['#cb2d3e', '#ef473a'], mode: 'act',
      desc: "Act like the hero, but don't say the line!" },
    // ---- Family Fun (universal) ----
    { id: 'kids', group: 'family', name: 'Kids (Easy)', emoji: '🧸', colors: ['#f7971e', '#ffd200'], mode: 'say',
      desc: 'Simple words for little ones and grandparents. Everyone can play!' },
    { id: 'cartoons', group: 'family', name: 'Cartoons', emoji: '🦄', colors: ['#ff6e7f', '#bfe9ff'], mode: 'say',
      desc: 'Cartoon and animated favourites. Do the voice!' },
    { id: 'heroes', group: 'family', name: 'Heroes & Villains', emoji: '🦸', colors: ['#0f2027', '#2c5364'], mode: 'say',
      desc: 'Superheroes, wizards, spies and the baddies they fight.' },
    { id: 'stories', group: 'family', name: 'Fairy Tales', emoji: '🏰', colors: ['#a770ef', '#fdb99b'], mode: 'say',
      desc: 'Once upon a time… fairy tales, fables and magic.' },
    { id: 'games', group: 'family', name: 'Games & Toys', emoji: '🎮', colors: ['#6a3093', '#a044ff'], mode: 'say',
      desc: 'Board games, playground games, video games and toys.' },
    { id: 'school', group: 'family', name: 'School Days', emoji: '📚', colors: ['#2980b9', '#6dd5fa'], mode: 'say',
      desc: 'Homework, exams, lunch boxes and the last bell.' },
    { id: 'nature', group: 'family', name: 'Nature & Space', emoji: '🌈', colors: ['#00c9ff', '#92fe9d'], mode: 'say',
      desc: 'Weather, wild places, planets and stars.' },
    { id: 'myths', group: 'family', name: 'Myths & Legends', emoji: '🐉', colors: ['#ff512f', '#dd2476'], mode: 'say',
      desc: 'Gods, heroes, monsters and magical creatures.' },
    // ---- Festivals & Traditions (pick a country) ----
    { id: 'festivals', group: 'culture', name: 'Festivals & Holidays', emoji: '🎉', colors: ['#c31432', '#f0a500'], mode: 'say',
      desc: 'Celebrations, traditions and special days.' },
    { id: 'weddings', group: 'culture', name: 'Weddings', emoji: '💍', colors: ['#b24592', '#f15f79'], mode: 'say',
      desc: 'Everything about the big, fat wedding.' },
    { id: 'life', group: 'culture', name: 'Daily Life', emoji: '🏡', colors: ['#ff9966', '#ff5e62'], mode: 'say',
      desc: 'Everyday moments and habits everyone back home will recognise.' },
  ],
};

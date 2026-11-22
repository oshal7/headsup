/*
 * The app's categories and which regions each one has.
 *
 * A category with only "world" is universal (no country picker).
 * A category with country regions shows a flag picker ("Movies from which country?").
 * Card lists live in content/<region>.js; India reuses the web game's js/data.js.
 *
 * mode: 'say' (describe it), 'act' (no talking), 'hum' (hum the tune)
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

  categories: [
    // ---- Universal first ----
    { id: 'animals', name: 'Animals', emoji: '🐘', colors: ['#56ab2f', '#a8e063'], mode: 'say',
      desc: 'Animals from the farm, the jungle and the sea. Sounds and actions allowed!' },
    { id: 'objects', name: 'Everyday Things', emoji: '🧸', colors: ['#8e2de2', '#4a00e0'], mode: 'say',
      desc: 'Things you use every day, at home, at school and on the go.' },
    { id: 'food', name: 'Food & Drinks', emoji: '🍕', colors: ['#e65c00', '#f9d423'], mode: 'say',
      desc: 'Snacks, dishes and drinks. Warning: you will get hungry.' },
    { id: 'jobs', name: 'Jobs', emoji: '👩‍🚒', colors: ['#2193b0', '#6dd5ed'], mode: 'say',
      desc: 'What do they do all day? Describe or act out the job.' },
    { id: 'act', name: 'Act It Out', emoji: '🎭', colors: ['#00b09b', '#96c93d'], mode: 'act',
      desc: 'No talking! Act out the action for the guesser.' },
    { id: 'sports', name: 'Sports', emoji: '⚽', colors: ['#11998e', '#38ef7d'], mode: 'say',
      desc: 'Games, legends and famous moments.' },
    { id: 'places', name: 'Famous Places', emoji: '🗺️', colors: ['#f953c6', '#b91d73'], mode: 'say',
      desc: 'Landmarks, cities and wonders. Where in the world?' },
    { id: 'kids', name: 'Kids (Easy)', emoji: '🧒', colors: ['#f7971e', '#ffd200'], mode: 'say',
      desc: 'Simple words for little ones and grandparents. Everyone can play!' },
    { id: 'myths', name: 'Myths & Legends', emoji: '🐉', colors: ['#ff512f', '#dd2476'], mode: 'say',
      desc: 'Gods, heroes, monsters and magical creatures.' },
    { id: 'brands', name: 'Brands', emoji: '🛍️', colors: ['#4776e6', '#8e54e9'], mode: 'say',
      desc: 'Logos you know by heart. Sing the jingle if you dare!' },
    // ---- Country packs ----
    { id: 'movies', name: 'Movies', emoji: '🎬', colors: ['#ff3d6e', '#ff9a3d'], mode: 'say',
      desc: 'Describe the story without saying the title!' },
    { id: 'stars', name: 'Celebrities', emoji: '🌟', colors: ['#7b2ff7', '#f107a3'], mode: 'say',
      desc: 'Actors, singers and famous faces. Impressions allowed!' },
    { id: 'songs', name: 'Songs', emoji: '🎤', colors: ['#fc466b', '#3f5efb'], mode: 'hum',
      desc: 'Hum the tune, no lyrics allowed!' },
    { id: 'tv', name: 'TV Shows', emoji: '📺', colors: ['#3a1c71', '#ffaf7b'], mode: 'say',
      desc: 'Sitcoms, cartoons, reality shows and binge-worthy series.' },
    { id: 'festivals', name: 'Festivals & Holidays', emoji: '🎉', colors: ['#c31432', '#f0a500'], mode: 'say',
      desc: 'Celebrations, traditions and special days.' },
    { id: 'lines', name: 'Famous Movie Lines', emoji: '🗯️', colors: ['#cb2d3e', '#ef473a'], mode: 'act',
      desc: "Act like the hero, but don't say the line!" },
    { id: 'weddings', name: 'Weddings', emoji: '💍', colors: ['#b24592', '#f15f79'], mode: 'say',
      desc: 'Everything about the big, fat wedding.' },
    { id: 'life', name: 'Daily Life', emoji: '🏠', colors: ['#f12711', '#f5af19'], mode: 'say',
      desc: 'Everyday moments and habits everyone back home will recognise.' },
  ],
};

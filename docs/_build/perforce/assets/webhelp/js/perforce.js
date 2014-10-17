// Perforce Documentation Javascript

!function ($) {
  $(function() {

    var $window = $(window),
        $body   = $(document.body),
        navHeight = $('header').outerHeight(true) + 10;

    // if we get this far, we don't need the noscript block
    $("noscript").remove();
    // but we do need to see the other elements
    $("header").css("display", "block");
    $("footer").css("display", "block");
    $(".body").css("display", "block");

    $("#content").scrollspy({
      target: 'nav.toc',
      offset: navHeight
    });

    // hookup swiping for navigation
    $(window).touchwipe({
      wipeLeft: function() {
        if (!$("link[rel='next']").length) return;
        e.preventDefault();
        window.location = $("link[rel='next']").attr("href");
      },
      wipeRight: function() { 
        if (!$("link[rel='prev']").length) return;
        e.preventDefault();
        window.location = $("link[rel='prev']").attr("href");
      },
      preventDefaultEvents: false
    });

    // hookup menu
    $("button.toc").on("click", function (e) {
      e.preventDefault();
      p4.toggleMenu();
    });

    $("a.expander").on("click", function (e) {
      e.preventDefault();
      var pNode  = $(this).parent("li"),
          ulNode = $(this).next("ul");
      if (pNode.hasClass("expanded")) {
        pNode.removeClass("expanded");
        ulNode.css("maxHeight", "");
      }
      else {
        pNode.addClass("expanded");
        var ulHeight = 0;
        ulNode.find("li").each(function () {
          ulHeight += $(this).height();
        });
        ulNode.css("maxHeight", ulHeight + "px");
      }
    });

    // hookup search
    $("button.search").on("click", function (e) {
      e.preventDefault();
      p4.toggleSearch();
    });

    $("button.clear").on("click", function (e) {
      e.preventDefault();
      $("#search-text").val("");
      p4.search.unhighlight();
    });

    var searchCookie = "textToSearch";
    $("#search-text").on("keyup", function (e) {
      var results = p4.search.start($("#search-text").val());
      p4.search.unhighlight();
      p4.search.render(results);
      p4.search.highlight(p4.search.theWords);
    });

    $(".search-highlight a").click(function (e) {
      e.preventDefault();
      p4.search.toggleHighlight();
    });
    p4.search.highlight();

    // hookup n/p, and left/right arrows, for keyboard doc navigation
    $(window).on('keydown', function(e) {
      // don't act on special keys, already handled events, or text input areas
      if (e.altKey || e.ctrlKey || e.metaKey || e.isDefaultPrevented()
        || $(e.target).is('input, textarea, select')
      ) {
        return;
      }

      if (e.which === 78 || e.which === 39) { // n or right-arrow for next
        if (!$("link[rel='next']").length) return;
        e.preventDefault();
        window.location = $("link[rel='next']").attr("href");
      }
      else if (e.which === 80 || e.which === 37) { // p or left-arrow for previous
        if (!$("link[rel='prev']").length) return;
        e.preventDefault();
        window.location = $("link[rel='prev']").attr("href");
      }
      else if (e.which === 85) { // u
        if (!$("link[rel='up']").length) return;
        e.preventDefault();
        window.location = $("link[rel='up']").attr("href");
      }
      else if (e.which === 72) { // h
        if (!$("link[rel='top']").length) return;
        e.preventDefault();
        window.location = $("link[rel='top']").attr("href");
      }
      else if (e.which === 84) { // t
        e.preventDefault();
        document.body.scrollTop = document.documentElement.scrollTop = 0;
      }
      else if (e.which === 70 || e.which === 83) { // f or s
        e.preventDefault();
        p4.toggleSearch();
      }
    });
  });
}(window.jQuery);

// ====================================================================
// Customized Bootstrap ScrollSpy for Perforce documentation navigation
// ====================================================================

+function ($) { "use strict";

  // SCROLLSPY CLASS DEFINITION
  // ==========================

  function ScrollSpy(element, options) {
    var href
    var process  = $.proxy(this.process, this)

    this.$element       = $(element).is('body') ? $(window) : $(element)
    this.$body          = $('body')
    this.$scrollElement = this.$element.on('scroll.bs.scroll-spy.data-api', process)
    this.options        = $.extend({}, ScrollSpy.DEFAULTS, options)
    this.selector       = (this.options.target
      || ((href = $(element).attr('href')) && href.replace(/.*(?=#[^\s]+$)/, '')) //strip for ie7
      || '') + ' .nav li > a'
    this.offsets        = $([])
    this.targets        = $([])
    this.activeTarget   = null

    this.refresh()
    this.process()
  }

  ScrollSpy.DEFAULTS = {
    offset: 10
  }

  ScrollSpy.prototype.refresh = function () {
    var offsetMethod = this.$element[0] == window ? 'offset' : 'position'

    this.offsets = $([])
    this.targets = $([])

    var dURL = window.location.pathname.match(/\/?([^\/]+)$/)
    var self     = this
    var $targets = this.$body
      .find(this.selector)
      .map(function () {
        var $el    = $(this)
        var href   = $el.data('target') || $el.attr('href')
        var $href  = /^#\w/.test(href) && $(href)
        var href2  = /\/?([^/#]+)(#.+)$/.exec(href);
        var $href2 = href2 && href2[2] && dURL && href2[1] === dURL[1] && $(href2[2]);

        if (!$href2 || !$href2.length) {
          return null;
        }

        return (
          [[ $href2[offsetMethod]().top + (!$.isWindow(self.$scrollElement.get(0)) && self.$scrollElement.scrollTop()), href2[2] ]]
        ) || null;
      })
      .sort(function (a, b) { return a[0] - b[0] })
      .each(function () {
        self.offsets.push(this[0])
        self.targets.push(this[1])
      })
  }

  ScrollSpy.prototype.process = function () {
    var scrollTop    = this.$scrollElement.scrollTop() + this.options.offset
    var scrollHeight = this.$scrollElement[0].scrollHeight || this.$body[0].scrollHeight
    var maxScroll    = scrollHeight - this.$scrollElement.height()
    var offsets      = this.offsets
    var targets      = this.targets
    var activeTarget = this.activeTarget
    var i

    if (scrollTop >= maxScroll) {
      return activeTarget != (i = targets.last()[0]) && this.activate(i)
    }

    for (i = offsets.length; i--;) {
      activeTarget != targets[i]
        && scrollTop >= offsets[i]
        && (!offsets[i + 1] || scrollTop <= offsets[i + 1])
        && this.activate( targets[i] )
    }
  }

  ScrollSpy.prototype.activate = function (target) {
    this.activeTarget = target

    $(this.selector)
      .parents('.active')
      .removeClass('active')

    var selector = this.selector
      + '[data-target$="' + target + '"],'
      + this.selector + '[href$="' + target + '"]'

    var active = $(selector)
      .parents('li')
      .addClass('active')

    if (active.parent('.dropdown-menu').length)  {
      active = active
        .closest('li.dropdown')
        .addClass('active')
    }

    active.trigger('activate')
  }


  // SCROLLSPY PLUGIN DEFINITION
  // ===========================

  var old = $.fn.scrollspy

  $.fn.scrollspy = function (option) {
    return this.each(function () {
      var $this   = $(this)
      var data    = $this.data('bs.scrollspy')
      var options = typeof option == 'object' && option

      if (!data) $this.data('bs.scrollspy', (data = new ScrollSpy(this, options)))
      if (typeof option == 'string') data[option]()
    })
  }

  $.fn.scrollspy.Constructor = ScrollSpy


  // SCROLLSPY NO CONFLICT
  // =====================

  $.fn.scrollspy.noConflict = function () {
    $.fn.scrollspy = old
    return this
  }


  // SCROLLSPY DATA-API
  // ==================

  $(window).on('load', function () {
    $('[data-spy="scroll"]').each(function () {
      var $spy = $(this)
      $spy.scrollspy($spy.data())
    })
  })

}(window.jQuery);

// ====================================================================
// Customized Search facility, adapted from DocBook WebHelp code
// ====================================================================

p4 = {};

p4.toggleMenu = function() {
  var elem = $("nav");
  if (elem.hasClass("open")) {
    elem.removeClass("open");
    $("footer").removeClass("close");
    $("#content").removeClass("nofooter");
    $("button.toc").removeClass("active");
  }
  else {
    elem.addClass("open");
    $("footer").addClass("close");
    $("#content").addClass("nofooter");
    $("button.toc").addClass("active");
    $(".search-interface").removeClass("open");
    $("button.search").removeClass("active");
  }
};

p4.toggleSearch = function() {
  var elem = $(".search-interface");
  if (elem.hasClass("open")) {
    elem.removeClass("open");
    $("footer").removeClass("close");
    $("#content").removeClass("nofooter");
    $("button.search").removeClass("active");
    $("#search-text").blur();
  }
  else {
    elem.addClass("open");
    $("footer").addClass("close");
    $("#content").addClass("nofooter");
    $("button.search").addClass("active");
    $("#search-text").focus();
    $("nav").removeClass("open");
    $("button.toc").removeClass("active");
  }
};

p4.search = {
  cookieName: "p4doc_search",
  reAlpha:    /[^a-zA-Z]/g,
  reNumeric:  /[^0-9]/g,
  theWords:   "",

  start: function(searchFor) {
    if (searchFor === undefined || searchFor.length < 3) return;

    var wordList  = this.wordList(searchFor),
        stemmed   = this.stemmed(wordList),
        stemmedWordList = stemmed[0],
        stemmedMap  = stemmed[1];

    // compute scores per matching file
    var fileScores = {};
    for (i = 0; i < stemmedWordList.length; i++) {
      var stemmedWord = stemmedWordList[i].toString(),
          fileScoreString = w[stemmedWord],
          fileScoreList = fileScoreString !== undefined
            ? fileScoreString.split(",")
            : [];
      for (j = 0; j < fileScoreList.length; j++) {
        var fileAndScore  = fileScoreList[j].toString().split("*"),
            file          = fileAndScore[0].toString(),
            score         = parseInt(fileAndScore[1], 10);
        var fileScore = fileScores[file];
        if (fileScore === undefined) {
          var bits = fil[file].split("@@@");
          fileScores[file] = {
            url:          bits[0],
            title:        bits[1],
            description:  bits[2],
            words:        [],
            stemmed:      [],
            score:        0
          }
        }

        fileScores[file].score += score;
        fileScores[file].stemmed.push(stemmedWord);
        fileScores[file].words.push(stemmedMap[stemmedWord]);
      }
    }

    // collect results
    var results = [];
    for (i in fileScores) {
      if (fileScores.hasOwnProperty(i)) {
        results.push(fileScores[i]);
      }
    }

    // sort results, by score, by word count, finally by title
    results.sort(function(a, b) {
      // compare scores
      var diff = b.score - a.score;
      if (diff !== 0) return diff;

      // compare word count
      diff = b.words.length - a.words.length;
      if (diff !== 0) return diff;

      // compare titles
      var aTitle = a.title.replace(this.reAlpha, ""),
          bTitle = b.title.replace(this.reAlpha, "");
      if (aTitle === bTitle) {
        // compare titles numerically
        aTitle = parseInt(a.title.replace(this.reNumeric, ""), 10);
        bTitle = parseInt(b.title.replace(this.reNumeric, ""), 10);
        return aTitle === bTitle ? 0 : aTitle > bTitle ? 1 : -1;
      }
      else {
        return aTitle > bTitle ? 1 : -1;
      }
    });

    return results;
  },

  // turn on highlighting of search terms
  highlight: function(searchText) {
    if (searchText === undefined) {
      searchText = $.cookie(this.cookieName);
      $("#search-text").val(searchText);
    }
    if (searchText !== undefined) {
      var wordList  = this.wordList(searchText),
          stemmed   = this.stemmed(wordList);
      wordList = this.unique(wordList.concat(stemmed[0]));
      $("#content").highlight(wordList);
    }
  },

  // render search results
  render: function(results) {
    if (results === undefined) results = [];
    var html = '';
    if (results.length > 0) {
      var barMax  = results[0].score,
          i;
      for (i = 0; i < results.length; i++) {
        var result = results[i];
        result.width = (result.score / barMax * 100).toFixed();
        html += $.templates(
          '<li>'
          + '<a href="{{:url}}">'
          + '<div class="title">{{:title}}</div>'
          + '</a>'
          + '</li>'
        ).render(result);
      }
    }

    $(".search-interface .results").html('').append($(html));
    $(".search-interface .count .number").html(results.length);
  },

  // compute word stems for each search term
  stemmed: function(wordList) {
    var i, j,
        stemmedMap = []
        stemmedWordList = [];
    for (i = 0; i < wordList.length; i++) {
      var word    = wordList[i],
          stemmed = p4.search.stemmer(word),
          target  = w[stemmed] !== undefined ? stemmed : word;

      stemmedMap[target] = word;
      stemmedWordList.push(target);
    }

    return [ this.unique(stemmedWordList), stemmedMap ];
  },

  // toggle highlighting of search terms
  toggleHighlight: function () {
    if ($(".highlight").length) {
      this.unhighlight();
    }
    else {
      this.highlight();
    }
  },

  // turn off highlighting of search terms
  unhighlight: function () {
    $("#content").unhighlight();
  },

  // remove duplicate array entries
  unique: function(list) {
    var hash  = {},
        uniq  = [],
        i, l;
    for (i = 0, l = list.length; i < l; ++i) {
      if (hash.hasOwnProperty(list[i])) continue;
      uniq.push(list[i]);
      hash[list[i]] = 1;
    }
    return uniq;
  },

  // convert a string into a space-separated list of words
  wordList: function(searchString) {
    var string = this.theWords;
    if (searchString !== undefined) {
      string  = $.trim(
                  searchString.replace(/[^ -z]/g, "")
                    .replace(/['"\/]/g, " ")
                    .replace(/\.$/, "")
                    .replace(/  +/g, " ")
                );
      this.theWords = string;
      $.cookie(this.cookieName, string);
    }

    return string.split(" ").sort();
  }
};

p4.search.stemmer = (function() {
  var step2list = {
      "ational" : "ate",
      "tional" : "tion",
      "enci" : "ence",
      "anci" : "ance",
      "izer" : "ize",
      "bli" : "ble",
      "alli" : "al",
      "entli" : "ent",
      "eli" : "e",
      "ousli" : "ous",
      "ization" : "ize",
      "ation" : "ate",
      "ator" : "ate",
      "alism" : "al",
      "iveness" : "ive",
      "fulness" : "ful",
      "ousness" : "ous",
      "aliti" : "al",
      "iviti" : "ive",
      "biliti" : "ble",
      "logi" : "log"
    },

    step3list = {
      "icate" : "ic",
      "ative" : "",
      "alize" : "al",
      "iciti" : "ic",
      "ical" : "ic",
      "ful" : "",
      "ness" : ""
    },

    c = "[^aeiou]",          // consonant
    v = "[aeiouy]",          // vowel
    C = c + "[^aeiouy]*",    // consonant sequence
    V = v + "[aeiou]*",      // vowel sequence

    mgr0 = "^(" + C + ")?" + V + C,               // [C]VC... is m>0
    meq1 = "^(" + C + ")?" + V + C + "(" + V + ")?$",  // [C]VC[V] is m=1
    mgr1 = "^(" + C + ")?" + V + C + V + C,       // [C]VCVC... is m>1
    s_v = "^(" + C + ")?" + v;                   // vowel in stem

  return function (w) {
    var stem,
        suffix,
        firstch,
        re,
        re2,
        re3,
        re4,
        origword = w;

    if (w.length < 3) return w;

    firstch = w.substr(0,1);
    if (firstch == "y") {
      w = firstch.toUpperCase() + w.substr(1);
    }

    // Step 1a
    re = /^(.+?)(ss|i)es$/;
    re2 = /^(.+?)([^s])s$/;

    if (re.test(w)) { w = w.replace(re,"$1$2"); }
    else if (re2.test(w)) { w = w.replace(re2,"$1$2"); }

    // Step 1b
    re = /^(.+?)eed$/;
    re2 = /^(.+?)(ed|ing)$/;
    if (re.test(w)) {
      var fp = re.exec(w);
      re = new RegExp(mgr0);
      if (re.test(fp[1])) {
        re = /.$/;
        w = w.replace(re,"");
      }
    }
    else if (re2.test(w)) {
      var fp = re2.exec(w);
      stem = fp[1];
      re2 = new RegExp(s_v);
      if (re2.test(stem)) {
        w = stem;
        re2 = /(at|bl|iz)$/;
        re3 = new RegExp("([^aeiouylsz])\\1$");
        re4 = new RegExp("^" + C + v + "[^aeiouwxy]$");
        if (re2.test(w)) { w = w + "e"; }
        else if (re3.test(w)) { re = /.$/; w = w.replace(re,""); }
        else if (re4.test(w)) { w = w + "e"; }
      }
    }

    // Step 1c
    re = new RegExp("^(.+" + c + ")y$");
    if (re.test(w)) {
      var fp = re.exec(w);
      stem = fp[1];
      w = stem + "i";
    }

    // Step 2
    re = /^(.+?)(ational|tional|enci|anci|izer|bli|alli|entli|eli|ousli|ization|ation|ator|alism|iveness|fulness|ousness|aliti|iviti|biliti|logi)$/;
    if (re.test(w)) {
      var fp = re.exec(w);
      stem = fp[1];
      suffix = fp[2];
      re = new RegExp(mgr0);
      if (re.test(stem)) {
        w = stem + step2list[suffix];
      }
    }

    // Step 3
    re = /^(.+?)(icate|ative|alize|iciti|ical|ful|ness)$/;
    if (re.test(w)) {
      var fp = re.exec(w);
      stem = fp[1];
      suffix = fp[2];
      re = new RegExp(mgr0);
      if (re.test(stem)) {
        w = stem + step3list[suffix];
      }
    }

    // Step 4
    re = /^(.+?)(al|ance|ence|er|ic|able|ible|ant|ement|ment|ent|ou|ism|ate|iti|ous|ive|ize)$/;
    re2 = /^(.+?)(s|t)(ion)$/;
    if (re.test(w)) {
      var fp = re.exec(w);
      stem = fp[1];
      re = new RegExp(mgr1);
      if (re.test(stem)) {
        w = stem;
      }
    }
    else if (re2.test(w)) {
      var fp = re2.exec(w);
      stem = fp[1] + fp[2];
      re2 = new RegExp(mgr1);
      if (re2.test(stem)) {
        w = stem;
      }
    }

    // Step 5
    re = /^(.+?)e$/;
    if (re.test(w)) {
      var fp = re.exec(w);
      stem = fp[1];
      re = new RegExp(mgr1);
      re2 = new RegExp(meq1);
      re3 = new RegExp("^" + C + v + "[^aeiouwxy]$");
      if (re.test(stem) || (re2.test(stem) && !(re3.test(stem)))) {
        w = stem;
      }
    }

    re = /ll$/;
    re2 = new RegExp(mgr1);
    if (re.test(w) && re2.test(w)) {
      re = /.$/;
      w = w.replace(re,"");
    }

    // and turn initial Y back to y
    if (firstch == "y") {
      w = firstch.toLowerCase() + w.substr(1);
    }

    // See http://snowball.tartarus.org/algorithms/english/stemmer.html
    // "Exceptional forms in general"
    var specialWords = {
      "skis":   "ski",
      "skies":  "sky",
      "dying":  "die",
      "lying":  "lie",
      "tying":  "tie",
      "idly":   "idl",
      "gently": "gentl",
      "ugly" :  "ugli",
      "early":  "earli",
      "only":   "onli",
      "singly": "singl"
    };

    if(specialWords[origword]){
      w = specialWords[origword];
    }

    if ("sky news howe atlas cosmos bias andes inning outing canning herring earring proceed exceed succeed".indexOf(origword) !== -1) {
      w = origword;
    }

    // Address words overstemmed as gener-
    re = /.*generate?s?d?(ing)?$/;
    if (re.test(origword)) {
      w = w + 'at';
    }
    re = /.*general(ly)?$/;
    if (re.test(origword)) {
      w = w + 'al';
    }
    re = /.*generic(ally)?$/;
    if (re.test(origword)) {
      w = w + 'ic';
    }
    re = /.*generous(ly)?$/;
    if (re.test(origword)) {
      w = w + 'ous';
    }
    // Address words overstemmed as commun-
    re = /.*communit(ies)?y?/;
    if (re.test(origword)) {
      w = w + 'iti';
    }

    return w;
  }
})();

// prime the word index
var w = new Object();

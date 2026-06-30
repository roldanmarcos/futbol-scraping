package com.futbol.scraping.service;

import com.futbol.scraping.dto.PlayerDTO;
import com.futbol.scraping.dto.PlayerDetailDTO;
import com.futbol.scraping.dto.QuoteDTO;
import com.futbol.scraping.exception.ResourceNotFoundException;
import com.futbol.scraping.model.Player;
import com.futbol.scraping.model.PlayerQuote;
import com.futbol.scraping.repository.PlayerQuoteRepository;
import com.futbol.scraping.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerQuoteRepository playerQuoteRepository;

    @Cacheable("players")
    public List<PlayerDTO> getPlayers(String league, String team, String position) {
        Specification<Player> spec = Specification.where(null);
        if (league != null && !league.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("league")), league.toLowerCase()));
        }
        if (team != null && !team.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("team")), "%" + team.toLowerCase() + "%"));
        }
        if (position != null && !position.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("position")), position.toLowerCase()));
        }

        return playerRepository.findAll(spec).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    @Cacheable(value = "playerDetail", key = "#id")
    public PlayerDetailDTO getPlayerById(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));

        Optional<PlayerQuote> latestQuote = playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player);
        List<PlayerQuote> recentQuotes = playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(player)
                .stream().limit(10).toList();

        return PlayerDetailDTO.builder()
                .id(player.getId())
                .name(player.getName())
                .league(player.getLeague())
                .team(player.getTeam())
                .position(player.getPosition())
                .age(player.getAge())
                .weight(player.getWeight())
                .appearances(player.getAppearances())
                .goals(player.getGoals())
                .assists(player.getAssists())
                .whoscoredId(player.getWhoscoredId())
                .url(player.getUrl())
                .currentQuote(latestQuote.map(PlayerQuote::getValue).orElse(null))
                .lastQuoteDate(latestQuote.map(PlayerQuote::getQuoteDate).orElse(null))
                .recentQuotes(recentQuotes.stream().map(this::toQuoteDTO).toList())
                .build();
    }

    @Transactional
    @CacheEvict(value = { "players", "playerDetail", "ranking" }, allEntries = true)
    public Player savePlayer(Player player) {
        return playerRepository.save(player);
    }

    @Transactional
    @CacheEvict(value = { "players", "playerDetail", "ranking" }, allEntries = true)
    public Player saveOrUpdatePlayer(Player player) {
        if (player.getWhoscoredId() != null) {
            Optional<Player> existing = playerRepository.findByWhoscoredId(player.getWhoscoredId());
            if (existing.isPresent()) {
                Player p = existing.get();
                p.setName(player.getName());
                p.setLeague(player.getLeague());
                p.setTeam(player.getTeam());
                p.setPosition(player.getPosition());
                p.setAge(player.getAge());
                p.setWeight(player.getWeight());
                p.setAppearances(player.getAppearances());
                p.setGoals(player.getGoals());
                p.setAssists(player.getAssists());
                p.setPositionText(player.getPositionText());
                p.setPlayedPositions(player.getPlayedPositions());
                p.setPlayedPositionsShort(player.getPlayedPositionsShort());
                p.setTeamRegionName(player.getTeamRegionName());
                p.setRegionCode(player.getRegionCode());
                p.setHeight(player.getHeight());
                p.setSubOn(player.getSubOn());
                p.setManOfTheMatch(player.getManOfTheMatch());
                p.setMinutesPlayed(player.getMinutesPlayed());
                p.setIsManOfTheMatch(player.getIsManOfTheMatch());
                p.setIsActive(player.getIsActive());
                p.setIsOpta(player.getIsOpta());
                p.setTournamentShortName(player.getTournamentShortName());
                p.setTournamentId(player.getTournamentId());
                p.setTournamentName(player.getTournamentName());
                p.setTournamentRegionId(player.getTournamentRegionId());
                p.setTournamentRegionCode(player.getTournamentRegionCode());
                p.setTournamentRegionName(player.getTournamentRegionName());
                p.setSeasonId(player.getSeasonId());
                p.setSeasonName(player.getSeasonName());
                p.setRating(player.getRating());
                p.setShotsPerGame(player.getShotsPerGame());
                p.setAerialWonPerGame(player.getAerialWonPerGame());
                p.setYellowCard(player.getYellowCard());
                p.setRedCard(player.getRedCard());
                p.setPassSuccess(player.getPassSuccess());
                p.setRanking(player.getRanking());
                p.setPlayerId(player.getPlayerId());
                p.setFirstName(player.getFirstName());
                p.setLastName(player.getLastName());
                p.setTeamId(player.getTeamId());
                p.setUrl(player.getUrl());
                p.setLastScrapedAt(System.currentTimeMillis());
                return playerRepository.save(p);
            }
        }
        return playerRepository.save(player);
    }

    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }

    private PlayerDTO toDTO(Player player) {
        Optional<PlayerQuote> latestQuote = playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player);
        return PlayerDTO.builder()
                .id(player.getId())
                .name(player.getName())
                .league(player.getLeague())
                .team(player.getTeam())
                .position(player.getPosition())
                .age(player.getAge())
                .appearances(player.getAppearances())
                .goals(player.getGoals())
                .assists(player.getAssists())
                .whoscoredId(player.getWhoscoredId())
                .currentQuote(latestQuote.map(PlayerQuote::getValue).orElse(null))
                .lastQuoteDate(latestQuote.map(PlayerQuote::getQuoteDate).orElse(null))
                .build();
    }

    private QuoteDTO toQuoteDTO(PlayerQuote quote) {
        return QuoteDTO.builder()
                .id(quote.getId())
                .playerId(quote.getPlayer().getId())
                .playerName(quote.getPlayer().getName())
                .value(quote.getValue())
                .quoteDate(quote.getQuoteDate())
                .strategyVersion(quote.getStrategyVersion())
                .baseScore(quote.getBaseScore())
                .build();
    }
}

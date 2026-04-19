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
import java.util.stream.Collectors;

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
                .collect(Collectors.toList());
    }

    @Cacheable(value = "playerDetail", key = "#id")
    public PlayerDetailDTO getPlayerById(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + id));

        Optional<PlayerQuote> latestQuote = playerQuoteRepository.findTopByPlayerOrderByQuoteDateDesc(player);
        List<PlayerQuote> recentQuotes = playerQuoteRepository.findByPlayerOrderByQuoteDateDesc(player)
                .stream().limit(10).collect(Collectors.toList());

        return PlayerDetailDTO.builder()
                .id(player.getId())
                .name(player.getName())
                .league(player.getLeague())
                .team(player.getTeam())
                .position(player.getPosition())
                .nationality(player.getNationality())
                .age(player.getAge())
                .weight(player.getWeight())
                .appearances(player.getAppearances())
                .goals(player.getGoals())
                .assists(player.getAssists())
                .whoscoredId(player.getWhoscoredId())
                .url(player.getUrl())
                .currentQuote(latestQuote.map(PlayerQuote::getValue).orElse(null))
                .lastQuoteDate(latestQuote.map(PlayerQuote::getQuoteDate).orElse(null))
                .recentQuotes(recentQuotes.stream().map(this::toQuoteDTO).collect(Collectors.toList()))
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
                p.setNationality(player.getNationality());
                if (player.getAge() != null)
                    p.setAge(player.getAge());
                if (player.getWeight() != null)
                    p.setWeight(player.getWeight());
                if (player.getAppearances() != null)
                    p.setAppearances(player.getAppearances());
                if (player.getGoals() != null)
                    p.setGoals(player.getGoals());
                if (player.getAssists() != null)
                    p.setAssists(player.getAssists());
                if (player.getPositionText() != null)
                    p.setPositionText(player.getPositionText());
                if (player.getPlayedPositions() != null)
                    p.setPlayedPositions(player.getPlayedPositions());
                if (player.getPlayedPositionsShort() != null)
                    p.setPlayedPositionsShort(player.getPlayedPositionsShort());
                if (player.getTeamRegionName() != null)
                    p.setTeamRegionName(player.getTeamRegionName());
                if (player.getRegionCode() != null)
                    p.setRegionCode(player.getRegionCode());
                if (player.getHeight() != null)
                    p.setHeight(player.getHeight());
                if (player.getSubOn() != null)
                    p.setSubOn(player.getSubOn());
                if (player.getManOfTheMatch() != null)
                    p.setManOfTheMatch(player.getManOfTheMatch());
                if (player.getMinutesPlayed() != null)
                    p.setMinutesPlayed(player.getMinutesPlayed());
                if (player.getIsManOfTheMatch() != null)
                    p.setIsManOfTheMatch(player.getIsManOfTheMatch());
                if (player.getIsActive() != null)
                    p.setIsActive(player.getIsActive());
                if (player.getIsOpta() != null)
                    p.setIsOpta(player.getIsOpta());
                if (player.getTournamentShortName() != null)
                    p.setTournamentShortName(player.getTournamentShortName());
                if (player.getTournamentId() != null)
                    p.setTournamentId(player.getTournamentId());
                if (player.getTournamentName() != null)
                    p.setTournamentName(player.getTournamentName());
                if (player.getTournamentRegionId() != null)
                    p.setTournamentRegionId(player.getTournamentRegionId());
                if (player.getTournamentRegionCode() != null)
                    p.setTournamentRegionCode(player.getTournamentRegionCode());
                if (player.getTournamentRegionName() != null)
                    p.setTournamentRegionName(player.getTournamentRegionName());
                if (player.getSeasonId() != null)
                    p.setSeasonId(player.getSeasonId());
                if (player.getSeasonName() != null)
                    p.setSeasonName(player.getSeasonName());
                if (player.getRating() != null)
                    p.setRating(player.getRating());
                if (player.getShotsPerGame() != null)
                    p.setShotsPerGame(player.getShotsPerGame());
                if (player.getAerialWonPerGame() != null)
                    p.setAerialWonPerGame(player.getAerialWonPerGame());
                if (player.getYellowCard() != null)
                    p.setYellowCard(player.getYellowCard());
                if (player.getRedCard() != null)
                    p.setRedCard(player.getRedCard());
                if (player.getPassSuccess() != null)
                    p.setPassSuccess(player.getPassSuccess());
                if (player.getRanking() != null)
                    p.setRanking(player.getRanking());
                if (player.getPlayerId() != null)
                    p.setPlayerId(player.getPlayerId());
                if (player.getFirstName() != null)
                    p.setFirstName(player.getFirstName());
                if (player.getLastName() != null)
                    p.setLastName(player.getLastName());
                if (player.getTeamId() != null)
                    p.setTeamId(player.getTeamId());
                if (player.getUrl() != null)
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
                .nationality(player.getNationality())
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

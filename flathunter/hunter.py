"""Default Flathunter implementation for the command line"""
import logging
from itertools import chain

from flathunter.config import Config
from flathunter.filter import Filter
from flathunter.processor import ProcessorChain
from flathunter.pubsub.nop_pubsub import NopPubsub


class Hunter:
    """Hunter class - basic methods for crawling and processing / filtering exposes"""
    __log__ = logging.getLogger('flathunt')

    def __init__(self, config, searchers, id_watch, pubsub=NopPubsub()):
        self.config = config
        self.searchers = searchers
        if not isinstance(self.config, Config):
            raise Exception("Invalid config for hunter - should be a 'Config' object")
        self.id_watch = id_watch
        self.pubsub = pubsub

    def crawl_for_exposes(self, max_pages=None):
        """Trigger a new crawl of the configured URLs"""
        return chain(*[searcher.crawl(url, max_pages)
                       for searcher in self.searchers
                       for url in self.config.urls()])

    def hunt_flats(self, max_pages=None):
        """Crawl, process and filter exposes"""
        filter_set = Filter.builder() \
            .read_config(self.config) \
            .filter_already_seen(self.id_watch) \
            .build()

        processor_chain = ProcessorChain.builder(self.config) \
            .save_all_exposes(self.id_watch) \
            .apply_filter(filter_set) \
            .resolve_addresses(self.searchers) \
            .calculate_durations() \
            .publish_exposes(self.pubsub) \
            .build()

        result = []
        # We need to iterate over this list to force the evaluation of the pipeline
        for expose in processor_chain.process(self.crawl_for_exposes(max_pages)):
            self.__log__.info('New offer: %s', expose['title'])
            result.append(expose)

        return result
